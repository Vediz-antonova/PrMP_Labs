package com.vedizL.mobilelabs.ui.settings

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.auth.AuthManager
import com.vedizL.mobilelabs.data.auth.BiometricAuthManager
import com.vedizL.mobilelabs.data.preferences.ThemeKeys
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager
import yuku.ambilwarna.AmbilWarnaDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePreferences
    private lateinit var switchBiometric: Switch
    private lateinit var tvBiometricStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = ThemePreferences(this)
        ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        
        setContentView(R.layout.activity_settings)

        BiometricAuthManager.init(this)

        switchBiometric = findViewById(R.id.switchBiometric)
        tvBiometricStatus = findViewById(R.id.tvBiometricStatus)

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

        setupBiometricSection()
    }

    private fun setupBiometricSection() {
        val email = AuthManager.getUserEmail()

        if (AuthManager.isAnonymous() || email.isEmpty()) {
            tvBiometricStatus.text = "Login to enable biometric authentication"
            tvBiometricStatus.setTextColor(Color.YELLOW)
            switchBiometric.isEnabled = false
            return
        }

        val isEnabled = BiometricAuthManager.isBiometricEnabledForEmail(email)
        switchBiometric.isChecked = isEnabled
        tvBiometricStatus.text = if (isEnabled)
            "Biometric login enabled for $email"
        else
            "Biometric login disabled"
        tvBiometricStatus.setTextColor(if (isEnabled) Color.GREEN else Color.GRAY)

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showEnableBiometricDialog(email)
            } else {
                showDisableBiometricDialog(email)
            }
        }
    }

    private fun showEnableBiometricDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Enable Biometric Login")
            .setMessage("You'll need to verify your identity to enable fingerprint login for $email")
            .setPositiveButton("Continue") { _, _ ->
                BiometricAuthManager.showBiometricPrompt(
                    this,
                    "Verify Identity",
                    "Authenticate to enable biometric login",
                    "Place your finger on the sensor",
                    onSuccess = {
                        val token = BiometricAuthManager.generateBiometricToken()
                        BiometricAuthManager.saveBiometricToken(email, token)
                        BiometricAuthManager.setBiometricEnabledForEmail(email, true)
                        BiometricAuthManager.setLastBiometricEmail(email)

                        switchBiometric.isChecked = true
                        tvBiometricStatus.text = "Biometric login enabled for $email"
                        tvBiometricStatus.setTextColor(Color.GREEN)
                        Toast.makeText(this, "Biometric login enabled", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        switchBiometric.isChecked = false
                        Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                switchBiometric.isChecked = false
            }
            .show()
    }

    private fun showDisableBiometricDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Disable Biometric Login")
            .setMessage("Are you sure you want to disable fingerprint login for $email?")
            .setPositiveButton("Yes") { _, _ ->
                BiometricAuthManager.setBiometricEnabledForEmail(email, false)
                BiometricAuthManager.clearBiometricForEmail(email)
                switchBiometric.isChecked = false
                tvBiometricStatus.text = "Biometric login disabled"
                tvBiometricStatus.setTextColor(Color.GRAY)
                Toast.makeText(this, "Biometric login disabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { _, _ ->
                switchBiometric.isChecked = true
            }
            .show()
    }

    private fun openColorPicker(key: String) {
        val initialColor = prefs.getColor(key, Color.WHITE)

        val dialog = AmbilWarnaDialog(this, initialColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                prefs.saveColor(key, color)
                prefs.setCustomThemeEnabled(true)
                Toast.makeText(this@SettingsActivity, "Color saved", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {}
        })

        dialog.show()
    }

}