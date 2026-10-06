package com.vitor.melembre.localweb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vitor.melembre.MainActivity
import com.vitor.melembre.MeLembreApp
import com.vitor.melembre.R

class LocalAccessService : Service() {
    private var server: LocalAccessServer? = null
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAccess()
            else -> startAccess(
                host = intent?.getStringExtra(EXTRA_HOST).orEmpty(),
                pin = intent?.getStringExtra(EXTRA_PIN).orEmpty(),
            )
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        val current = LocalAccessController.state.value
        if (current.active || current.starting) {
            LocalAccessController.markOff()
        }
        super.onDestroy()
    }

    private fun startAccess(host: String, pin: String) {
        ensureChannel()
        startInForeground()
        if (!LocalIp.isUsableLanIpv4(host)) {
            failStart(LocalAccessController.NO_WIFI_MESSAGE)
            return
        }
        if (pin.length != 4) {
            failStart("Não foi possível abrir o acesso na rede.")
            return
        }
        try {
            server?.stop()
            server = null
            val actions = RepositoryReminderActions((application as MeLembreApp).reminderRepository)
            val next = LocalAccessServer(host, LocalAccessController.PORT, pin, actions)
            next.start()
            server = next
            LocalAccessController.markOn("http://$host:${LocalAccessController.PORT}", pin)
        } catch (_: Exception) {
            failStart("Não foi possível abrir o acesso na rede.")
        }
    }

    private fun failStart(message: String) {
        server?.stop()
        server = null
        LocalAccessController.markError(message)
        leaveForeground()
        stopSelf()
    }

    private fun stopAccess() {
        server?.stop()
        server = null
        LocalAccessController.markOff()
        leaveForeground()
        stopSelf()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        inForeground = true
    }

    private fun leaveForeground() {
        if (!inForeground) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        inForeground = false
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Acesso pelo computador",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Aviso enquanto o acesso local pelo computador está ativo"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopPending = PendingIntent.getService(
            this,
            1,
            Intent(this, LocalAccessService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPending = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Lembre-me — acesso pelo computador ativo")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(0, "Desativar", stopPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "acesso_local"
        private const val NOTIFICATION_ID = 8765
        private const val ACTION_START = "com.vitor.melembre.localweb.START"
        private const val ACTION_STOP = "com.vitor.melembre.localweb.STOP"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PIN = "pin"

        fun start(context: Context, host: String, pin: String) {
            val intent = Intent(context, LocalAccessService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PIN, pin)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalAccessService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
