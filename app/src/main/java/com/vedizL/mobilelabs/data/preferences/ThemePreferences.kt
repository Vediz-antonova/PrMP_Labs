package com.vedizL.mobilelabs.data.preferences

import android.content.Context
import androidx.core.content.edit

class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun saveThemeMode(mode: String) {
        prefs.edit { putString("theme_mode", mode) }
    }

    fun getThemeMode(): String =
        prefs.getString("theme_mode", "light") ?: "light"

    fun saveColor(key: String, color: Int) {
        prefs.edit { putInt(key, color) }
    }

    fun getColor(key: String, default: Int): Int =
        prefs.getInt(key, default)

    fun setCustomThemeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("custom_theme_enabled", enabled) }
    }

    fun isCustomThemeEnabled(): Boolean =
        prefs.getBoolean("custom_theme_enabled", false)

    fun isTutorialShown(): Boolean =
        prefs.getBoolean("tutorial_shown", false)

    fun setTutorialShown() {
        prefs.edit { putBoolean("tutorial_shown", true) }
    }
}