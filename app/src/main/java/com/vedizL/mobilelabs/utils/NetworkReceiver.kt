package com.vedizL.mobilelabs.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)

        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (connected) {
            NotificationHelper.send(
                context,
                "Internet Restored",
                "Database access is available again.",
                1001
            )
        } else {
            NotificationHelper.send(
                context,
                "Internet Lost",
                "Database access is unavailable.",
                1002
            )
        }
    }
}