package com.vedizL.mobilelabs

import android.app.Application
import com.vedizL.mobilelabs.data.auth.AuthManager

class MobileLabsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
    }
}