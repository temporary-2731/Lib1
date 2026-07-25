package com.yourname.vf.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtil {
    fun getPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                val file = File(context.cacheDir, displayName ?: "temp_video")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                return file.absolutePath
            }
        }
        return null
    }
}
