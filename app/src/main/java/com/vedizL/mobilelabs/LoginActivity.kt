package com.vedizL.mobilelabs

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.vedizL.mobilelabs.data.auth.AuthManager
import com.vedizL.mobilelabs.data.auth.BiometricAuthManager
import android.content.res.Configuration
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnBiometricLogin: Button
    private lateinit var btnGuestLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = ThemePreferences(this)
        ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnBiometricLogin = findViewById(R.id.btnBiometricLogin)
        btnGuestLogin = findViewById(R.id.btnGuestLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        auth = FirebaseAuth.getInstance()
        BiometricAuthManager.init(this)

        val savedEmail = BiometricAuthManager.getLastBiometricEmail()
        if (savedEmail.isNotEmpty()) {
            etEmail.setText(savedEmail)
            if (BiometricAuthManager.isBiometricEnabledForEmail(savedEmail)) {
                etPassword.requestFocus()
                etPassword.postDelayed({
                    showBiometricLoginPrompt(savedEmail)
                }, 500)
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performEmailPasswordLogin(email, password)
        }

        btnBiometricLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!BiometricAuthManager.isBiometricEnabledForEmail(email)) {
                Toast.makeText(this, "Biometric not enabled for this account. Please login with password first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showBiometricLoginPrompt(email)
        }

        btnGuestLogin.setOnClickListener {
            AuthManager.setAnonymousMode(true)
            startMainActivity()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showBiometricLoginPrompt(email: String) {
        BiometricAuthManager.showBiometricPrompt(
            this,
            "Login with Biometric",
            "Verify your identity for $email",
            "Use your fingerprint",
            onSuccess = {
                val token = BiometricAuthManager.getBiometricToken(email)
                if (token != null && BiometricAuthManager.verifyBiometricToken(email, token)) {
                    performBiometricLoginSuccess(email)
                } else {
                    Toast.makeText(this, "Biometric token invalid. Please login with password.", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun performBiometricLoginSuccess(email: String) {
        AuthManager.setUserLoggedIn(email)
        Toast.makeText(this, "Biometric login successful!", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }

    private fun performEmailPasswordLogin(email: String, password: String) {
        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    AuthManager.setUserLoggedIn(email)

                    if (!BiometricAuthManager.isBiometricEnabledForEmail(email)) {
                        offerBiometricEnrollment(email)
                    } else {
                        startMainActivity()
                    }
                } else {
                    Toast.makeText(this, "Login error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun offerBiometricEnrollment(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Enable Biometric Login?")
            .setMessage("Would you like to enable fingerprint recognition for faster login next time?")
            .setPositiveButton("Yes") { _, _ ->
                enableBiometricForAccount(email)
            }
            .setNegativeButton("No") { _, _ ->
                startMainActivity()
            }
            .show()
    }

    private fun enableBiometricForAccount(email: String) {
        BiometricAuthManager.showBiometricPrompt(
            this,
            "Enable Biometric Login",
            "Verify your identity to enable biometric login",
            "Place your finger on the sensor",
            onSuccess = {
                val token = BiometricAuthManager.generateBiometricToken()
                BiometricAuthManager.saveBiometricToken(email, token)
                BiometricAuthManager.setBiometricEnabledForEmail(email, true)
                BiometricAuthManager.setLastBiometricEmail(email)

                Toast.makeText(this, "Biometric login enabled successfully!", Toast.LENGTH_SHORT).show()
                startMainActivity()
            },
            onError = { error ->
                Toast.makeText(this, "Failed to enable biometric: $error", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }
        )
    }

    private fun showForgotPasswordDialog() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("A password reset link will be sent to $email")
            .setPositiveButton("Send Reset Link") { _, _ ->
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AlertDialog.Builder(this)
                        .setTitle("Check Your Email")
                        .setMessage("Password reset link has been sent to $email")
                        .setPositiveButton("OK") { _, _ -> }
                        .show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}