package com.yourname.vf.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourname.vf.databinding.ActivityProgressBinding
import com.yourname.vf.manager.ConversionManager
import com.yourname.vf.service.VideoConverterService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPauseResume.setOnClickListener {
            val isPaused = ConversionManager.progress.value.isPaused
            val intent = Intent(this, VideoConverterService::class.java).apply {
                action = if (isPaused) VideoConverterService.ACTION_RESUME else VideoConverterService.ACTION_PAUSE
            }
            startService(intent)
        }

        binding.btnCancel.setOnClickListener {
            startService(Intent(this, VideoConverterService::class.java).apply {
                action = VideoConverterService.ACTION_CANCEL
            })
            finish()
        }

        lifecycleScope.launch {
            ConversionManager.progress.collectLatest { progress ->
                val pct = if (progress.totalChunks > 0) (progress.overallChunksCompleted * 100 / progress.totalChunks) else 0
                binding.progressMain.progress = pct
                binding.tvPercentage.text = "$pct%"
                binding.tvDataTransferred.text = formatBytes(progress.currentChunkBytesProcessed) + " / " + formatBytes(progress.currentChunkTotalBytes)
                binding.tvElapsed.text = formatDuration(progress.elapsedMs)
                binding.tvRemaining.text = formatDuration(progress.estimatedRemainingMs)
                binding.btnPauseResume.text = if (progress.isPaused) "Resume" else "Pause"

                if (!progress.isRunning && !progress.isPaused) {
                    finish()
                }
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024f * 1024f)
        return "%.1f MB".format(mb)
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
