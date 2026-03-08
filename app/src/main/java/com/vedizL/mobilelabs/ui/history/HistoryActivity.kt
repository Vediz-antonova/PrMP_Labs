package com.vedizL.mobilelabs.ui.history

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.history.ActionHistoryStore
import com.vedizL.mobilelabs.data.history.ActionEvent
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.ui.theme.ThemeManager

class HistoryActivity : AppCompatActivity() {
    private lateinit var prefs: ThemePreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = ThemePreferences(this)
        
        if (prefs.isFollowSystemTheme()) {
            ThemeManager.applySystemTheme()
        } else {
            ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        }
        
        setContentView(R.layout.activity_history)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        val items = ActionHistoryStore.load(this)
        val adapter = HistoryAdapter(items) { event: ActionEvent ->
            val value = event.details.substringAfterLast("=")
            val result = if (value.isNotEmpty()) value else ""
            if (result.isNotEmpty()) {
                val data = Intent()
                data.putExtra("selected_value", result)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
        rv.adapter = adapter

        // If cloud history exists, load and replace local
        ActionHistoryStore.loadCloud(this) { cloudItems ->
            if (cloudItems.isNotEmpty()) {
                runOnUiThread {
                    adapter.submitList(cloudItems)
                    if (cloudItems.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                    }
                }
            }
        }

        if (items.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndApplySystemTheme()
    }

    private fun checkAndApplySystemTheme() {
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
        if (prefs.isFollowSystemTheme()) {
            ThemeManager.applySystemTheme()
        } else {
            ThemeManager.applyDefaultTheme(prefs.getThemeMode())
        }
    }
}
