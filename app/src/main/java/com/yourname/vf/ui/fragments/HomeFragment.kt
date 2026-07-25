package com.yourname.vf.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.yourname.vf.R
import com.yourname.vf.databinding.FragmentHomeBinding
import com.yourname.vf.model.ConversionState
import com.yourname.vf.service.VideoConverterService
import com.yourname.vf.utils.FileUtil

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedVideoPath = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)

        binding.cardPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO)
        }

        binding.spinnerMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val method = parent?.getItemAtPosition(position).toString()
                binding.layoutTimeIntensive.visibility = if (method == "Time Intensive") View.VISIBLE else View.GONE
                binding.layoutManual.visibility = if (method == "Manual") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnStart.setOnClickListener {
            if (selectedVideoPath.isEmpty()) return@setOnClickListener
            val method = binding.spinnerMethod.selectedItem.toString()
            val params = getConversionParams(method)
            val state = ConversionState(
                inputFilePath = selectedVideoPath,
                outputDir = "/storage/emulated/0/VFOutput",
                method = method,
                startTrimSec = 0.0,
                endTrimSec = 120.0,
                totalChunks = 0,
                completedChunks = emptyList(),
                currentChunkIndex = 0,
                crf = params.first,
                preset = params.second,
                resolutionWidth = params.third,
                resolutionHeight = params.fourth,
                yaw = params.fifth,
                pitch = params.sixth,
                roll = params.seventh,
                fov = params.eighth,
                segmentDurationSec = 60
            )
            val intent = Intent(requireContext(), VideoConverterService::class.java).apply {
                action = VideoConverterService.ACTION_START
                putExtra("state", Gson().toJson(state))
            }
            requireContext().startService(intent)
            startActivity(Intent(requireContext(), com.yourname.vf.ui.ProgressActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
    }

    private fun getConversionParams(method: String): List<Any> {
        when (method) {
            "Normal" -> return listOf(23, "medium", 1280, 720, 0f, 0f, 0f, 100f)
            "Fast" -> return listOf(30, "ultrafast", 640, 360, 0f, 0f, 0f, 90f)
            "Manual" -> {
                val crf = binding.etCrf.text.toString().toIntOrNull() ?: 23
                val preset = binding.etPreset.text.toString().ifBlank { "medium" }
                val w = binding.etWidth.text.toString().toIntOrNull() ?: 1280
                val h = binding.etHeight.text.toString().toIntOrNull() ?: 720
                val yaw = binding.etYaw.text.toString().toFloatOrNull() ?: 0f
                val pitch = binding.etPitch.text.toString().toFloatOrNull() ?: 0f
                val roll = binding.etRoll.text.toString().toFloatOrNull() ?: 0f
                val fov = binding.etFov.text.toString().toFloatOrNull() ?: 100f
                return listOf(crf, preset, w, h, yaw, pitch, roll, fov)
            }
            "Time Intensive" -> {
                val checkedId = binding.radioTimeLevel.checkedRadioButtonId
                val levelText = if (checkedId != -1) {
                    val radio = view?.findViewById<RadioButton>(checkedId)
                    radio?.text.toString()
                } else "Level 1 (Standard)"
                return when {
                    levelText.contains("Level 1") -> listOf(23, "medium", 1280, 720, 0f, 0f, 0f, 100f)
                    levelText.contains("Level 2") -> listOf(20, "slow", 1280, 720, 0f, 0f, 0f, 100f)
                    levelText.contains("Level 3") -> listOf(18, "slower", 1920, 1080, 0f, 0f, 0f, 110f)
                    levelText.contains("Level 4") -> listOf(15, "veryslow", 1920, 1080, 0f, 0f, 0f, 120f)
                    else -> listOf(23, "medium", 1280, 720, 0f, 0f, 0f, 100f)
                }
            }
        }
        return listOf(23, "medium", 1280, 720, 0f, 0f, 0f, 100f)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedVideoPath = FileUtil.getPath(requireContext(), uri) ?: uri.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_VIDEO = 1001
    }
}
