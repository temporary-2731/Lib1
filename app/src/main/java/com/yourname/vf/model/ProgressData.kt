package com.yourname.vf.model

data class ProgressData(
    val overallChunksCompleted: Int,
    val totalChunks: Int,
    val currentChunkBytesProcessed: Long,
    val currentChunkTotalBytes: Long,
    val elapsedMs: Long,
    val estimatedRemainingMs: Long,
    val isRunning: Boolean,
    val isPaused: Boolean
)
