package com.yourname.vf.manager

import com.google.gson.Gson
import com.yourname.vf.model.ConversionState
import com.yourname.vf.model.ProgressData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object ConversionManager {
    private val _state = MutableStateFlow<ConversionState?>(null)
    val state: StateFlow<ConversionState?> = _state

    private val _progress = MutableStateFlow(ProgressData(
        overallChunksCompleted = 0,
        totalChunks = 0,
        currentChunkBytesProcessed = 0,
        currentChunkTotalBytes = 0,
        elapsedMs = 0,
        estimatedRemainingMs = 0,
        isRunning = false,
        isPaused = false
    ))
    val progress: StateFlow<ProgressData> = _progress

    private val _isActive = AtomicBoolean(true)

    fun isActive(): Boolean = _isActive.get()
    fun setPaused(paused: Boolean) {
        _isActive.set(!paused)
        _progress.value = _progress.value.copy(isPaused = paused)
    }
    fun setState(s: ConversionState) {
        _state.value = s
        _isActive.set(true)
        _progress.value = _progress.value.copy(
            totalChunks = s.totalChunks,
            isRunning = true,
            isPaused = false
        )
    }

    fun updateState(transform: (ConversionState) -> ConversionState) {
        _state.value = _state.value?.let(transform)
    }

    fun completeChunk(index: Int) {
        updateState { it.copy(
            completedChunks = it.completedChunks + index,
            currentChunkIndex = it.currentChunkIndex + 1
        ) }
        _progress.value = _progress.value.copy(
            overallChunksCompleted = _state.value?.completedChunks?.size ?: 0
        )
    }

    fun updateChunkProgress(bytesProcessed: Long, totalBytes: Long, elapsedMs: Long, estimatedRemainingMs: Long) {
        _progress.value = _progress.value.copy(
            currentChunkBytesProcessed = bytesProcessed,
            currentChunkTotalBytes = totalBytes,
            elapsedMs = elapsedMs,
            estimatedRemainingMs = estimatedRemainingMs
        )
    }

    fun finishConversion() {
        _progress.value = ProgressData(0,0,0,0,0,0, false, false)
    }

    fun saveStateToFile(directory: File) {
        val json = Gson().toJson(_state.value)
        File(directory, "conversion_state.json").writeText(json)
    }

    fun loadState(directory: File): ConversionState? {
        val file = File(directory, "conversion_state.json")
        return if (file.exists()) Gson().fromJson(file.readText(), ConversionState::class.java) else null
    }
}
