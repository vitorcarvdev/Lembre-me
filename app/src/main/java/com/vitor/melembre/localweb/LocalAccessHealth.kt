package com.vitor.melembre.localweb

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Confere se o servidor do acesso local realmente está aceitando conexões.
 * O Android derruba o socket quando o Wi-Fi renegocia, mas a thread do servidor continua
 * "viva" girando a 100% de CPU, então "isAlive" sozinho não basta: precisa conectar de verdade.
 */
object LocalAccessHealth {
    const val ANY_HOST = "0.0.0.0"

    fun isListening(port: Int, timeoutMs: Int = 1500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
