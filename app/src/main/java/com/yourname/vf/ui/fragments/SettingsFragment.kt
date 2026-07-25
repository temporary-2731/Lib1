package com.yourname.vf.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.yourname.vf.R
import com.yourname.vf.databinding.FragmentSettingsBinding
import com.yourname.vf.utils.ThemeHelper

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)

        val currentTheme = ThemeHelper.getTheme(requireContext())
        when (currentTheme) {
            0 -> binding.radioTheme.check(R.id.radio_dark_blue)
            1 -> binding.radioTheme.check(R.id.radio_pure_black)
            2 -> binding.radioTheme.check(R.id.radio_blue)
            3 -> binding.radioTheme.check(R.id.radio_white)
        }

        binding.radioTheme.setOnCheckedChangeListener { _, checkedId ->
            val index = when (checkedId) {
                R.id.radio_dark_blue -> 0
                R.id.radio_pure_black -> 1
                R.id.radio_blue -> 2
                R.id.radio_white -> 3
                else -> 0
            }
            ThemeHelper.setTheme(requireContext(), index)
            requireActivity().recreate()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedPath = prefs.getString("output_dir", "/storage/emulated/0/VFOutput")
        binding.etOutputPath.setText(savedPath)

        binding.btnBrowseFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_OUTPUT_FOLDER)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OUTPUT_FOLDER && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit().putString("output_uri", uri.toString()).apply()
                binding.etOutputPath.setText(uri.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_OUTPUT_FOLDER = 2001
    }
}
