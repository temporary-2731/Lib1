package com.yourname.vf.utils

import android.app.Activity
import android.content.Context
import com.yourname.vf.R

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"

    fun setTheme(context: Context, themeIndex: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, themeIndex).apply()
    }

    fun getTheme(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, 0)
    }

    fun applyTheme(activity: Activity) {
        when (getTheme(activity)) {
            0 -> activity.setTheme(R.style.Theme_VF_DarkBlue)
            1 -> activity.setTheme(R.style.Theme_VF_PureBlack)
            2 -> activity.setTheme(R.style.Theme_VF_Blue)
            3 -> activity.setTheme(R.style.Theme_VF_White)
            else -> activity.setTheme(R.style.Theme_VF_DarkBlue)
        }
    }
}
