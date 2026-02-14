package com.vedizL.mobilelabs.data.preferences

import android.content.Context
import androidx.core.content.edit

class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun saveThemeMode(mode: String) {
        prefs.edit { putString("theme_mode", mode) }
    }

    fun getThemeMode(): String =
        prefs.getString("theme_mode", "system") ?: "system"

    fun saveColor(key: String, color: Int) {
        prefs.edit { putInt(key, color) }
    }

    fun getColor(key: String, default: Int): Int =
        prefs.getInt(key, default)

    fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }
}