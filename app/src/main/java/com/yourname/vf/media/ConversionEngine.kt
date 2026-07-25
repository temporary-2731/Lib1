package com.yourname.vf.media

import com.yourname.vf.manager.ConversionManager
import com.yourname.vf.model.ConversionState
import kotlinx.coroutines.*
import java.io.File

class ConversionEngine(private val state: ConversionState) {
    private var job: Job? = null
    private var pipeline: MediaPipeline? = null

    suspend fun start(onProgress: (progress: Float, elapsedMs: Long, remainingMs: Long) -> Unit) {
        job = CoroutineScope(Dispatchers.Default).launch {
            try {
                val outputDir = File(state.outputDir).apply { mkdirs() }
                val baseName = File(state.inputFilePath).nameWithoutExtension
                val outputFile = File(outputDir, "flat_$baseName.mp4")
                pipeline = MediaPipeline(state.inputFilePath, outputFile.absolutePath, state)
                pipeline?.convert { progress, elapsed, remaining ->
                    ConversionManager.updateChunkProgress(0, 0, elapsed, remaining)
                    onProgress(progress, elapsed, remaining)
                }
                ConversionManager.finishConversion()
            } catch (e: Exception) {
                ConversionManager.finishConversion()
            }
        }
    }

    fun pause() {
        pipeline?.cancel()
        job?.cancel()
        ConversionManager.setPaused(true)
    }

    fun resume() {}
    fun cancel() {
        pipeline?.cancel()
        job?.cancel()
        ConversionManager.finishConversion()
    }
}
