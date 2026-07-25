package com.yourname.vf.media

import android.media.*
import android.opengl.*
import android.view.Surface
import com.yourname.vf.model.ConversionState
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class MediaPipeline(
    private val inputPath: String,
    private val outputPath: String,
    private val state: ConversionState
) {
    private var extractor: MediaExtractor? = null
    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var audioExtractor: MediaExtractor? = null
    private var videoTrackIdx = -1
    private var audioTrackIdx = -1
    private var encoderTrackIdx = -1
    private var audioOutputTrackIdx = -1
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var shader: V360Shader? = null
    private var inputTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var decoderSurface: Surface? = null
    private var encoderSurface: Surface? = null
    private var frameCount = 0
    private var totalFrames = 0
    private var isCancelled = false

    suspend fun convert(
        onProgress: (progress: Float, elapsedMs: Long, remainingMs: Long) -> Unit
    ) = withContext(Dispatchers.Default) {
        try {
            setup()
            totalFrames = estimateTotalFrames()
            val startTime = System.currentTimeMillis()
            decoder?.start()
            encoder?.start()

            if (audioTrackIdx >= 0 && audioOutputTrackIdx >= 0) {
                feedAudioPassThrough()
            }

            while (!isCancelled) {
                val inputBufferIndex = decoder?.dequeueInputBuffer(10_000L) ?: break
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder?.getInputBuffer(inputBufferIndex) ?: continue
                    val sampleSize = extractor?.readSampleData(inputBuffer, 0) ?: -1
                    if (sampleSize < 0) {
                        decoder?.queueInputBuffer(inputBufferIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        break
                    }
                    val pts = extractor?.sampleTime ?: 0
                    decoder?.queueInputBuffer(inputBufferIndex, 0, sampleSize, pts, 0)
                    extractor?.advance()
                }

                val info = MediaCodec.BufferInfo()
                while (true) {
                    val outIndex = decoder?.dequeueOutputBuffer(info, 0) ?: break
                    if (outIndex >= 0) {
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoder?.signalEndOfInputStream()
                            break
                        }
                        renderFrame(info.presentationTimeUs)
                        frameCount++
                        val progress = if (totalFrames > 0) frameCount.toFloat() / totalFrames else 0f
                        val elapsed = System.currentTimeMillis() - startTime
                        val remaining = if (frameCount > 0) (elapsed * (totalFrames - frameCount) / frameCount) else 0L
                        withContext(Dispatchers.Main) {
                            onProgress(progress.coerceIn(0f, 1f), elapsed, remaining)
                        }
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {}
                }
            }

            drainEncoder()
            muxer?.stop()
            muxer?.release()
            extractor?.release()
            decoder?.stop(); decoder?.release()
            encoder?.stop(); encoder?.release()
            audioExtractor?.release()
            releaseEgl()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun cancel() { isCancelled = true }

    private fun setup() {
        extractor = MediaExtractor().apply {
            setDataSource(inputPath)
            videoTrackIdx = selectVideoTrack()
            selectTrack(videoTrackIdx)
        }
        val videoFormat = extractor!!.getTrackFormat(videoTrackIdx)
        decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME)!!)
        inputTextureId = GlUtil.createTexture()
        surfaceTexture = SurfaceTexture(inputTextureId)
        decoderSurface = Surface(surfaceTexture)
        decoder?.configure(videoFormat, decoderSurface, null, 0)

        val encoderFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, state.resolutionWidth, state.resolutionHeight
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 5000000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder?.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoderSurface = encoder?.createInputSurface()

        muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        audioExtractor = MediaExtractor().apply {
            setDataSource(inputPath)
            audioTrackIdx = selectAudioTrack()
            if (audioTrackIdx >= 0) {
                selectTrack(audioTrackIdx)
                val af = getTrackFormat(audioTrackIdx)
                audioOutputTrackIdx = muxer?.addTrack(af) ?: -1
            }
        }

        initEgl()
        shader = V360Shader().apply { build() }
    }

    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, IntArray(1), 0)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGLExt.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], encoderSurface,
            intArrayOf(EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun releaseEgl() {
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
        surfaceTexture?.release()
    }

    private fun renderFrame(presentationTimeUs: Long) {
        surfaceTexture?.updateTexImage()
        shader?.use()
        shader?.bindTexture(inputTextureId)
        shader?.setSize(
            extractor?.getTrackFormat(videoTrackIdx)?.getInteger(MediaFormat.KEY_WIDTH) ?: 1920,
            extractor?.getTrackFormat(videoTrackIdx)?.getInteger(MediaFormat.KEY_HEIGHT) ?: 960,
            state.resolutionWidth,
            state.resolutionHeight
        )
        shader?.setRotation(state.yaw, state.pitch, state.roll)
        shader?.setFov(state.fov)

        GLES20.glViewport(0, 0, state.resolutionWidth, state.resolutionHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GlUtil.drawQuad(shader!!)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun feedAudioPassThrough() {
        CoroutineScope(Dispatchers.IO).launch {
            val buf = ByteArray(65536)
            while (!isCancelled) {
                val size = audioExtractor?.readSampleData(ByteBuffer.wrap(buf), 0) ?: break
                if (size < 0) break
                val info = MediaCodec.BufferInfo()
                info.set(0, size, audioExtractor?.sampleTime ?: 0, audioExtractor?.sampleFlags ?: 0)
                muxer?.writeSampleData(audioOutputTrackIdx, ByteBuffer.wrap(buf, 0, size), info)
                audioExtractor?.advance()
            }
        }
    }

    private fun drainEncoder() {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val outIndex = encoder?.dequeueOutputBuffer(info, 10_000L) ?: break
            if (outIndex >= 0) {
                val data = encoder?.getOutputBuffer(outIndex) ?: continue
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    encoder?.releaseOutputBuffer(outIndex, false)
                    continue
                }
                if (info.size != 0) {
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    muxer?.writeSampleData(encoderTrackIdx, data, info)
                }
                encoder?.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                encoderTrackIdx = muxer?.addTrack(encoder?.outputFormat) ?: -1
                muxer?.start()
            }
        }
    }

    private fun MediaExtractor.selectVideoTrack(): Int {
        for (i in 0 until trackCount) {
            val format = getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) return i
        }
        return -1
    }

    private fun MediaExtractor.selectAudioTrack(): Int {
        for (i in 0 until trackCount) {
            val format = getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) return i
        }
        return -1
    }

    private fun estimateTotalFrames(): Int {
        val format = extractor?.getTrackFormat(videoTrackIdx) ?: return 0
        val durationUs = format.getLong(MediaFormat.KEY_DURATION)
        val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE).takeIf { it > 0 } ?: 30
        return ((durationUs / 1_000_000f) * frameRate).toInt()
    }
}
