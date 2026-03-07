package com.vedizL.mobilelabs.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit
import androidx.security.crypto.MasterKeys

object AuthManager {
    private const val PREFS_NAME = "secure_auth_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_IS_ANONYMOUS = "is_anonymous"

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
            throw IllegalStateException("AuthManager not initialized. Call init() first.")
        }
        return encryptedPrefs!!
    }

    fun setUserLoggedIn(email: String) {
        val cleanEmail = email.replace("@", "_at_").replace(".", "_dot_")
        getPrefs().edit { putString(KEY_USER_EMAIL, email) }
        getPrefs().edit { putString(KEY_USER_ID, cleanEmail) }
        getPrefs().edit { putBoolean(KEY_IS_ANONYMOUS, false) }
    }

    fun isAnonymous(): Boolean {
        return getPrefs().getBoolean(KEY_IS_ANONYMOUS, true)
    }

    fun getUserId(): String {
        return getPrefs().getString(KEY_USER_ID, "anonymous") ?: "anonymous"
    }

    fun getUserEmail(): String {
        return getPrefs().getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        getPrefs().edit { clear() }
        setAnonymousMode(true)
    }

    fun setAnonymousMode(enabled: Boolean) {
        getPrefs().edit { putBoolean(KEY_IS_ANONYMOUS, enabled) }
        if (enabled) {
            val anonymousId = "anonymous_${System.currentTimeMillis()}"
            getPrefs().edit { putString(KEY_USER_ID, anonymousId) }
            getPrefs().edit { remove(KEY_USER_EMAIL) }
        }
    }
}