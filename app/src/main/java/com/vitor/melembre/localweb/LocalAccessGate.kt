package com.vitor.melembre.localweb

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class LocalAccessGate(private val pin: String) {
    private val tokens = ConcurrentHashMap.newKeySet<String>()
    private val random = SecureRandom()

    fun unlock(attempt: String): String? {
        if (attempt.trim() != pin) return null
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
}
