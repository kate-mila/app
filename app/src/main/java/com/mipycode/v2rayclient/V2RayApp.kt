package com.mipycode.v2rayclient

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class V2RayApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VPN_NOTIFICATION_CHANNEL,
                "اتصال VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "وضعیت اتصال V2Ray"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val VPN_NOTIFICATION_CHANNEL = "v2ray_vpn_channel"
        lateinit var instance: V2RayApp
            private set
    }
}
