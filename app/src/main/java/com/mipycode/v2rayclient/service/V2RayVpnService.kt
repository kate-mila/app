package com.mipycode.v2rayclient.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.mipycode.v2rayclient.MainActivity
import com.mipycode.v2rayclient.R
import com.mipycode.v2rayclient.V2RayApp
import com.mipycode.v2rayclient.data.db.AppDatabase
import com.mipycode.v2rayclient.data.model.ServerConfig
import com.mipycode.v2rayclient.util.XrayConfigBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * سرویس اصلی VPN. یک TUN interface می‌سازد و فایل‌دیسکریپتور آن را مستقیماً به
 * هسته‌ی Xray (متد StartLoop) می‌دهد. در نسخه‌ی فعلی AndroidLibXrayLite دیگر نیازی
 * به tun2socks جداگانه نیست — خود هسته بسته‌های IP رو از fd می‌خونه و پردازش می‌کنه.
 */
class V2RayVpnService : VpnService(), V2RayCallback {

    private var tunInterface: ParcelFileDescriptor? = null
    private var core: CoreController? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var currentServer: ServerConfig? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverId = intent.getLongExtra(EXTRA_SERVER_ID, -1L)
                if (serverId != -1L) {
                    serviceScope.launch {
                        val server = AppDatabase.getInstance(this@V2RayVpnService).serverDao().getById(serverId)
                        if (server != null) {
                            connect(server)
                        } else {
                            VpnStatus.setState(VpnStatus.State.ERROR, "سرور پیدا نشد")
                            stopSelf()
                        }
                    }
                }
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    private var statsJob: kotlinx.coroutines.Job? = null

    private fun connect(server: ServerConfig) {
        currentServer = server
        startForeground(NOTIFICATION_ID, buildNotification("در حال اتصال به ${server.remark}…"))

        serviceScope.launch {
            // اول TUN interface رو می‌سازیم تا fd معتبر داشته باشیم
            val fd = establishTunInterface(server)
            if (fd == null) {
                VpnStatus.setState(VpnStatus.State.ERROR, "ساخت رابط VPN ناموفق بود (مجوز؟)")
                stopSelf()
                return@launch
            }

            val configJson = XrayConfigBuilder.build(server)
            core = RealCoreController(this@V2RayVpnService)
            val started = core?.startCore(configJson, fd) ?: false

            if (started) {
                VpnStatus.setState(VpnStatus.State.CONNECTED, server.remark)
                startForeground(NOTIFICATION_ID, buildNotification("متصل به ${server.remark}"))
                startStatsPolling()
            } else {
                VpnStatus.setState(VpnStatus.State.ERROR, "اتصال به هسته‌ی V2Ray ناموفق بود")
                tunInterface?.close()
                tunInterface = null
                stopSelf()
            }
        }
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (true) {
                val (up, down) = core?.queryStats() ?: (0L to 0L)
                VpnStatus.updateTraffic(up, down)
                kotlinx.coroutines.delay(1500)
            }
        }
    }

    /** رابط TUN را می‌سازد و فایل‌دیسکریپتور خام آن را برای پاس دادن به هسته برمی‌گرداند. */
    private fun establishTunInterface(server: ServerConfig): Int? {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(TUN_IPV4_ADDRESS, 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .setMtu(1500)

        // برنامه‌ی خودمان را از تانل مستثنی کن تا لوپ بی‌نهایت پیش نیاید
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) { /* نادیده بگیر */ }

        tunInterface = builder.establish() ?: return null
        // detachFd باعث می‌شه مالکیت fd به هسته منتقل بشه؛ خودمون دیگه نباید close کنیم
        // مگر با dup برای نگه‌داشتن نسخه‌ی محلی جهت disconnect تمیز.
        return tunInterface?.fd
    }

    private fun disconnect() {
        statsJob?.cancel()
        core?.stopCore()
        tunInterface?.close()
        tunInterface = null
        VpnStatus.setState(VpnStatus.State.DISCONNECTED, null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCoreLog(message: String) {
        VpnStatus.appendLog(message)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, V2RayApp.VPN_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_CONNECT = "com.mipycode.v2rayclient.CONNECT"
        const val ACTION_DISCONNECT = "com.mipycode.v2rayclient.DISCONNECT"
        const val EXTRA_SERVER_ID = "extra_server_id"
        private const val NOTIFICATION_ID = 1
        private const val TUN_IPV4_ADDRESS = "10.10.10.1"
    }
}
