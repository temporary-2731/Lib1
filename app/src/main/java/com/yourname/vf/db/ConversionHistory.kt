package com.yourname.vf.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputFileName: String,
    val outputFilePath: String,
    val method: String,
    val startTime: Long,
    val durationSec: Int,
    val status: String
)
