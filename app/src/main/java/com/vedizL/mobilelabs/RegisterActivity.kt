package com.vedizL.mobilelabs

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.vedizL.mobilelabs.data.auth.AuthManager
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = ThemePreferences(this)
        
        if (prefs.isFollowSystemTheme()) {
            ThemeManager.applySystemTheme()
        } else {
            ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        }
        
        setContentView(R.layout.activity_register)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        auth = FirebaseAuth.getInstance()

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    btnRegister.isEnabled = true
                    if (task.isSuccessful) {
                        AuthManager.setUserLoggedIn(email)

                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Registration error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndApplySystemTheme()
    }

    private fun checkAndApplySystemTheme() {
        val prefs = ThemePreferences(this)
        if (prefs.isFollowSystemTheme()) {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val currentMode = when (currentNightMode) {
                Configuration.UI_MODE_NIGHT_YES -> "dark"
                Configuration.UI_MODE_NIGHT_NO -> "light"
                else -> "light"
            }
            val savedMode = prefs.getThemeMode()
            if (savedMode != currentMode && savedMode != "system") {
                prefs.saveThemeMode(currentMode)
                recreate()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkAndApplySystemTheme()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val prefs = ThemePreferences(this)
        if (prefs.isFollowSystemTheme()) {
            ThemeManager.applySystemTheme()
        } else {
            ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        }
    }
}