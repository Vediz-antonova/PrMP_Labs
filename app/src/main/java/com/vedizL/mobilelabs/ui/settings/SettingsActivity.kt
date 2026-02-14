package com.vedizL.mobilelabs.ui.settings

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager
import yuku.ambilwarna.AmbilWarnaDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ThemePreferences(this)

        openColorPicker()
    }

    private fun openColorPicker() {
        val initialColor = prefs.getColor("primary_color", Color.BLUE)

        val dialog = AmbilWarnaDialog(this, initialColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                prefs.saveColor("primary_color", color)
                ThemeManager.applyCustomColors(this@SettingsActivity)
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {}
        })

        dialog.show()
    }
}