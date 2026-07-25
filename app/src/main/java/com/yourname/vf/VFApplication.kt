package com.yourname.vf

import android.app.Application
import android.os.Environment
import android.widget.Toast
import com.yourname.vf.db.AppDatabase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VFApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashLog = sw.toString()

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "vf_crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"
                val file = File(downloadsDir, fileName)
                file.writeText(crashLog)

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(this, "Crash saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
