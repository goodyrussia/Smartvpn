package com.smartvpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVpnApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                VPN_CHANNEL_ID,
                "VPN Connection Status",
                NotificationManager.IMPORTANCE_LOW // Low importance prevents constant sound/vibration
            ).apply {
                description = "Shows the active VPN connection state and metrics"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(vpnChannel)
        }
    }

    companion object {
        const val VPN_CHANNEL_ID = "smart_vpn_status_channel"
    }
}
