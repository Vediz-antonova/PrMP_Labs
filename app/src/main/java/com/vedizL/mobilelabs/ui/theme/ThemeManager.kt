package com.vedizL.mobilelabs.ui.theme

import android.app.Activity
import android.graphics.PorterDuff
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.preferences.ThemeKeys
import com.vedizL.mobilelabs.data.preferences.ThemePreferences

object ThemeManager {

    fun applyDefaultTheme(mode: String) {
        when (mode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    fun applyCustomColors(activity: Activity) {
        val prefs = ThemePreferences(activity)

        if (!prefs.isCustomThemeEnabled()) return

        val display = activity.findViewById<TextView?>(R.id.tvDisplay)
        if (display == null) return

        val primary = prefs.getColor(
            ThemeKeys.PRIMARY,
            ContextCompat.getColor(activity, R.color.button_operation_text)
        )
        val buttonColor = prefs.getColor(
            ThemeKeys.BUTTON,
            ContextCompat.getColor(activity, R.color.button_number_bg)
        )
        val displayBg = prefs.getColor(
            ThemeKeys.DISPLAY_BG,
            ContextCompat.getColor(activity, R.color.calculator_background)
        )
        val displayText = prefs.getColor(
            ThemeKeys.DISPLAY_TEXT,
            ContextCompat.getColor(activity, R.color.display_text)
        )

        display.setBackgroundColor(displayBg)
        display.setTextColor(displayText)

        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide,
            R.id.btnPercent, R.id.btnDecimal, R.id.btnEquals,
            R.id.btnClear, R.id.btnPlusMinus, R.id.btnExpand,
            R.id.btnSqrtRow, R.id.btnSquareRow, R.id.btnPowerRow,
            R.id.btnFactorialRow
        )

        buttons.forEach { id ->
            val btn = activity.findViewById<Button?>(id) ?: return@forEach
            btn.background?.setColorFilter(buttonColor, PorterDuff.Mode.SRC_ATOP)
            btn.setTextColor(primary)
        }
    }
}