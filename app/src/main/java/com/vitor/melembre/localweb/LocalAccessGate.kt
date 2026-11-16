package com.vitor.melembre.localweb

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Porta do acesso pelo computador. Com 2 PINs errados seguidos o acesso trava por alguns minutos
 * (5, depois 10, 20, 40 e no máximo 60 se o bloqueio se repetir). O bloqueio vale para todo mundo,
 * porque pelo túnel todas as conexões chegam do mesmo endereço local.
 */
class LocalAccessGate(
    private val pin: String,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val tokens = ConcurrentHashMap.newKeySet<String>()
    private val random = SecureRandom()
    private var failures = 0
    private var lockouts = 0
    private var lockedUntil = 0L

    /** Segundos que ainda faltam para liberar novas tentativas (0 = liberado). */
    @Synchronized
    fun lockedForSeconds(): Long {
        val left = lockedUntil - now()
        return if (left > 0) (left + 999) / 1000 else 0L
    }

    @Synchronized
    fun unlock(attempt: String): String? {
        if (lockedForSeconds() > 0) return null
        if (attempt.trim() != pin) {
            failures++
            if (failures >= MAX_FAILURES) {
                failures = 0
                val minutes = minOf(BASE_LOCK_MINUTES shl lockouts.coerceAtMost(4), MAX_LOCK_MINUTES)
                lockouts++
                lockedUntil = now() + minutes * 60_000L
            }
            return null
        }
        failures = 0
        lockouts = 0
        val token = newToken()
        tokens.add(token)
        return token
    }

    fun allows(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return tokens.contains(token.trim())
    }

    private fun newToken(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        const val MAX_FAILURES = 2
        private const val BASE_LOCK_MINUTES = 5
        private const val MAX_LOCK_MINUTES = 60
    }
}
