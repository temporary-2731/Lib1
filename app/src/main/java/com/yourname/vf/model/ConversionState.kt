package com.yourname.vf.model

data class ConversionState(
    val inputFilePath: String,
    val outputDir: String,
    val method: String,
    val startTrimSec: Double,
    val endTrimSec: Double,
    val totalChunks: Int,
    val completedChunks: List<Int>,
    val currentChunkIndex: Int,
    val crf: Int,
    val preset: String,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val fov: Float,
    val segmentDurationSec: Int = 60
)
