package com.vedizL.mobilelabs.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.preferences.ThemeKeys
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import yuku.ambilwarna.AmbilWarnaDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = ThemePreferences(this)

        findViewById<Button>(R.id.btnPrimaryColor).setOnClickListener {
            openColorPicker(ThemeKeys.PRIMARY)
        }

        findViewById<Button>(R.id.btnButtonColor).setOnClickListener {
            openColorPicker(ThemeKeys.BUTTON)
        }

        findViewById<Button>(R.id.btnDisplayBg).setOnClickListener {
            openColorPicker(ThemeKeys.DISPLAY_BG)
        }

        findViewById<Button>(R.id.btnDisplayText).setOnClickListener {
            openColorPicker(ThemeKeys.DISPLAY_TEXT)
        }
    }

    private fun openColorPicker(key: String) {
        val initialColor = prefs.getColor(key, Color.WHITE)

        val dialog = AmbilWarnaDialog(this, initialColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                prefs.saveColor(key, color)
                prefs.setCustomThemeEnabled(true)
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {}
        })

        dialog.show()
    }
}