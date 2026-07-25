package com.yourname.vf.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.io.File
import java.io.FilenameFilter

class FilePickerDialog(
    private val onFileSelected: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = Environment.getExternalStorageDirectory()
        val videoFiles: Array<File> = root.listFiles(FilenameFilter { _, name ->
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
            name.endsWith(".mov") || name.endsWith(".ts") || name.endsWith(".m4v")
        }) ?: emptyArray()

        val fileNames = videoFiles.map { it.name }

        val listView = ListView(requireContext())
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fileNames)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Video")
            .setView(listView)
            .setNegativeButton("Cancel", null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = videoFiles[position].absolutePath
            onFileSelected(selected)
            dismiss()
        }

        return dialog
    }
}
