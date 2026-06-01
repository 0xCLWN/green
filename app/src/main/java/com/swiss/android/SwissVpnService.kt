package com.swiss.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import go.Seq
import libswiss.Libswiss
import java.io.File

@SuppressLint("VpnServicePolicy") // its intended
class SwissVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.swiss.android.START"
        const val ACTION_STOP = "com.swiss.android.STOP"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_DNS_SERVER = "dns_server"
        const val EXTRA_SOCKS_PORT = "socks_port"
        const val EXTRA_SOCKS_USER = "socks_user"
        const val EXTRA_SOCKS_PASS = "socks_pass"
        const val EXTRA_ALLOWED_APPS = "allowed_apps"
        private const val NOTIF_ID = 1
        private const val NOTIF_CHANNEL = "swiss_vpn"
        private const val TUN_ADDRESS = "198.18.0.1"
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn(
                intent.getStringExtra(EXTRA_CONFIG_JSON),
                intent.getStringExtra(EXTRA_DNS_SERVER) ?: "1.1.1.1",
                intent.getIntExtra(EXTRA_SOCKS_PORT, 10808),
                intent.getStringExtra(EXTRA_SOCKS_USER) ?: "",
                intent.getStringExtra(EXTRA_SOCKS_PASS) ?: "",
                intent.getStringArrayListExtra(EXTRA_ALLOWED_APPS) ?: emptyList(),
            )

            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(configJson: String?, dnsServer: String, socksPort: Int, socksUser: String, socksPass: String, allowedApps: List<String>) {
        if (configJson == null) {
            stopVpn(); return
        }

        startForeground(NOTIF_ID, buildNotification("Connecting…"))
        try {
            Seq.setContext(applicationContext)

            // Protect xray's outbound sockets so they bypass the TUN and don't loop.
            Libswiss.setProtector { fd -> protect(fd) }

            vpnInterface = Builder()
                .setSession("swiss")
                .setMtu(1500)
                .addAddress(TUN_ADDRESS, 30)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer(dnsServer)
                .also { b -> allowedApps.forEach { pkg -> runCatching { b.addAllowedApplication(pkg) } } }
                .establish()
                ?: return stopVpn()

            Libswiss.setAssetPath(filesDir.absolutePath) // GEO UPDATE — remove with GeoUpdater
            Libswiss.start(configJson)

            TProxyService.TProxyStartService(writeTunConfig(socksPort, socksUser, socksPass), vpnInterface!!.fd)

            startForeground(NOTIF_ID, buildNotification("Connected"))
            VpnState.status.value = VpnStatus.CONNECTED
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        VpnState.status.value = VpnStatus.DISCONNECTED
        runCatching { TProxyService.TProxyStopService() }
        runCatching { Libswiss.stop() }
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun writeTunConfig(socksPort: Int, socksUser: String, socksPass: String): String {
        val yaml = buildString {
            appendLine("tunnel:")
            appendLine("  mtu: 1500")
            appendLine("  ipv4: $TUN_ADDRESS")
            appendLine("socks5:")
            appendLine("  port: $socksPort")
            appendLine("  address: 127.0.0.1")
            appendLine("  udp: udp")
            if (socksUser.isNotEmpty()) {
                appendLine("  username: $socksUser")
                appendLine("  password: $socksPass")
            }
            appendLine("misc:")
            appendLine("  task-stack-size: 81920")
            appendLine("  connect-timeout: 5000")
            appendLine("  read-write-timeout: 60000")
            appendLine("  log-file: stderr")
            append("  log-level: warn")
        }
        return File(filesDir, "tun.yaml").also { it.writeText(yaml) }.absolutePath
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIF_CHANNEL, "VPN Status", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, SwissVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Swiss VPN")
            .setContentText(status)
            .addAction(0, "Disconnect", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
