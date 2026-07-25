package com.yourname.vf

import android.app.Application
import com.yourname.vf.db.AppDatabase

class VFApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
