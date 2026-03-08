package com.vedizL.mobilelabs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.vedizL.mobilelabs.data.auth.AuthManager
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = ThemePreferences(this)
        
        if (prefs.isFollowSystemTheme()) {
            ThemeManager.applySystemTheme()
        } else {
            ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        }
        
        setContentView(R.layout.activity_splash)

        AuthManager.init(this)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1000)
    }
}