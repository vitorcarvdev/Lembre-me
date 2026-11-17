package com.vitor.melembre.localweb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vitor.melembre.MainActivity
import com.vitor.melembre.MeLembreApp
import com.vitor.melembre.R
import kotlin.concurrent.thread

class LocalAccessService : Service() {
    private var server: LocalAccessServer? = null
    private var inForeground = false

    // Estado do acesso ativo: usado para recriar o servidor sozinho se ele cair
    private var running = false
    private var checking = false
    private var currentHost = ""
    private var currentPin = ""
    private val handler = Handler(Looper.getMainLooper())
    private var wifiLock: WifiManager.WifiLock? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val watchdog = object : Runnable {
        override fun run() {
            verifyAsync()
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent nulo = o Android recriou o serviço depois de derrubá-lo (START_STICKY)
        when (intent?.action) {
            ACTION_STOP -> stopAccess()
            ACTION_AUTO, null -> startAuto()
            else -> startAccess(
                host = intent.getStringExtra(EXTRA_HOST).orEmpty(),
                pin = intent.getStringExtra(EXTRA_PIN).orEmpty(),
                auto = false,
            )
        }
        return if (running) START_STICKY else START_NOT_STICKY
    }

    /** Religa sozinho (boot, atualização do app, serviço recriado): só se o usuário deixou o acesso ligado. */
    private fun startAuto() {
        if (!LocalAccessAuto.isEnabled(this)) {
            leaveForeground()
            stopSelf()
            return
        }
        startAccess(
            host = LocalIp.wifiIpv4(this).orEmpty(),
            pin = LocalPin.read(this),
            auto = true,
        )
    }

    override fun onDestroy() {
        releaseResources()
        val current = LocalAccessController.state.value
        if (current.active || current.starting) {
            LocalAccessController.markOff()
        }
        super.onDestroy()
    }

    private fun startAccess(host: String, pin: String, auto: Boolean) {
        ensureChannel()
        startInForeground()
        // Religando sozinho o Wi-Fi pode ainda não ter subido: o servidor escuta em todas as
        // interfaces e o IP mostrado é atualizado quando a rede chegar.
        if (!auto && !LocalIp.isUsableLanIpv4(host)) {
            failStart(LocalAccessController.NO_WIFI_MESSAGE)
            return
        }
        if (pin.length != 4) {
            failStart("Não foi possível abrir o acesso na rede.")
            return
        }
        currentHost = host.ifBlank { "localhost" }
        currentPin = pin
        if (!launchServer()) {
            failStart("Não foi possível abrir o acesso na rede.")
            return
        }
        // A escolha do usuário fica guardada; religar sozinho não a altera
        if (!auto) LocalAccessAuto.setEnabled(this, true)
        running = true
        acquireWifiLock()
        watchNetwork()
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_MS)
    }

    /**
     * Sobe o servidor em todas as interfaces (0.0.0.0). Antes ele ficava preso ao IP do Wi-Fi do
     * momento: quando o Wi-Fi renegociava, o Android derrubava o socket, o servidor morria e a
     * thread dele ficava girando a 100% de CPU sem atender ninguém.
     */
    private fun launchServer(): Boolean {
        return try {
            server?.stop()
            server = null
            val actions = RepositoryReminderActions((application as MeLembreApp).reminderRepository)
            val next = LocalAccessServer(LocalAccessHealth.ANY_HOST, LocalAccessController.PORT, currentPin, actions)
            next.start()
            server = next
            LocalAccessController.markOn("http://$currentHost:${LocalAccessController.PORT}", currentPin)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Vigia leve: uma conexão local rápida; só recria o servidor se ele realmente não responder. */
    private fun verifyAsync() {
        if (!running || checking) return
        checking = true
        thread(name = "LocalAccessHealth", isDaemon = true) {
            val healthy = LocalAccessHealth.isListening(LocalAccessController.PORT)
            handler.post {
                checking = false
                if (running && !healthy) {
                    launchServer()
                }
            }
        }
    }

    private fun watchNetwork() {
        if (networkCallback != null) return
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post { onNetworkChanged() }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                handler.post { onNetworkChanged() }
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (_: Exception) {
            // sem o aviso de rede, o vigia periódico continua cuidando do servidor
        }
    }

    private fun onNetworkChanged() {
        if (!running) return
        val ip = LocalIp.wifiIpv4(this)
        if (ip != null && ip != currentHost) {
            currentHost = ip
            LocalAccessController.markOn("http://$currentHost:${LocalAccessController.PORT}", currentPin)
        }
        verifyAsync()
    }

    @Suppress("DEPRECATION")
    private fun acquireWifiLock() {
        if (wifiLock?.isHeld == true) return
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        try {
            val lock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "melembre:acesso-local")
            lock.setReferenceCounted(false)
            lock.acquire()
            wifiLock = lock
        } catch (_: Exception) {
            // sem o lock o acesso funciona, só pode ficar mais lento com a tela apagada
        }
    }

    private fun releaseResources() {
        running = false
        checking = false
        handler.removeCallbacksAndMessages(null)
        networkCallback?.let { callback ->
            try {
                getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {
        }
        wifiLock = null
        server?.stop()
        server = null
    }

    private fun failStart(message: String) {
        releaseResources()
        LocalAccessController.markError(message)
        leaveForeground()
        stopSelf()
    }

    private fun stopAccess() {
        // desligado de propósito pelo usuário: não religa mais sozinho
        LocalAccessAuto.setEnabled(this, false)
        releaseResources()
        LocalAccessController.markOff()
        leaveForeground()
        stopSelf()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        try {
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
        } catch (_: Exception) {
            // religando em segundo plano o Android pode negar o modo em primeiro plano; o servidor
            // continua e o vigia tenta de novo na próxima partida
        }
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
        private const val WATCHDOG_MS = 30_000L
        private const val ACTION_START = "com.vitor.melembre.localweb.START"
        private const val ACTION_STOP = "com.vitor.melembre.localweb.STOP"
        private const val ACTION_AUTO = "com.vitor.melembre.localweb.AUTO"
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

        /** Religa o acesso sozinho (boot / atualização do app); não faz nada se o usuário deixou desligado. */
        fun startAuto(context: Context) {
            if (!LocalAccessAuto.isEnabled(context)) return
            val intent = Intent(context, LocalAccessService::class.java).apply {
                action = ACTION_AUTO
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // o Android pode negar a partida em segundo plano; o usuário religa pelo app
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocalAccessService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
