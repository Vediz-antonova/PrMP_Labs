package com.vedizL.mobilelabs.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.Executor
import androidx.core.content.edit

object BiometricAuthManager {
    private const val PREFS_NAME = "biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED_PREFIX = "biometric_enabled_"
    private const val KEY_BIOMETRIC_TOKEN_PREFIX = "biometric_token_"
    private const val KEY_LAST_EMAIL = "last_biometric_email"

    private var encryptedPrefs: SharedPreferences? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            encryptedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isInitialized = true
        }
    }

    private fun getPrefs(): SharedPreferences {
        if (!isInitialized) {
            throw IllegalStateException("BiometricAuthManager not initialized. Call init() first.")
        }
        return encryptedPrefs!!
    }

    fun isBiometricEnabledForEmail(email: String): Boolean {
        val key = KEY_BIOMETRIC_ENABLED_PREFIX + email.hashCode()
        return getPrefs().getBoolean(key, false)
    }

    fun setBiometricEnabledForEmail(email: String, enabled: Boolean) {
        val key = KEY_BIOMETRIC_ENABLED_PREFIX + email.hashCode()
        getPrefs().edit { putBoolean(key, enabled) }
    }

    fun saveBiometricToken(email: String, token: String) {
        val tokenKey = KEY_BIOMETRIC_TOKEN_PREFIX + email.hashCode()
        getPrefs().edit { putString(tokenKey, token) }
    }

    fun getBiometricToken(email: String): String? {
        val tokenKey = KEY_BIOMETRIC_TOKEN_PREFIX + email.hashCode()
        return getPrefs().getString(tokenKey, null)
    }

    fun setLastBiometricEmail(email: String) {
        getPrefs().edit { putString(KEY_LAST_EMAIL, email) }
    }

    fun getLastBiometricEmail(): String {
        return getPrefs().getString(KEY_LAST_EMAIL, "") ?: ""
    }

    fun clearBiometricForEmail(email: String) {
        val enabledKey = KEY_BIOMETRIC_ENABLED_PREFIX + email.hashCode()
        val tokenKey = KEY_BIOMETRIC_TOKEN_PREFIX + email.hashCode()

        getPrefs().edit {
            remove(enabledKey)
                .remove(tokenKey)
        }
    }

    fun generateBiometricToken(): String {
        return java.util.UUID.randomUUID().toString()
    }

    fun verifyBiometricToken(email: String, token: String): Boolean {
        val savedToken = getBiometricToken(email)
        return savedToken == token
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor: Executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError.invoke(errString.toString())
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}