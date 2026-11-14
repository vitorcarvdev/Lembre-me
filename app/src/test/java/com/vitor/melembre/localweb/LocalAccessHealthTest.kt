package com.vitor.melembre.localweb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class LocalAccessHealthTest {
    @Test
    fun detectsListeningServerBoundToAllInterfaces() {
        ServerSocket(0, 5, InetAddress.getByName(LocalAccessHealth.ANY_HOST)).use { socket ->
            assertTrue(LocalAccessHealth.isListening(socket.localPort))
        }
    }

    @Test
    fun detectsDeadServerOnceSocketIsClosed() {
        val port = ServerSocket(0, 5, InetAddress.getByName(LocalAccessHealth.ANY_HOST)).use { it.localPort }
        assertFalse(LocalAccessHealth.isListening(port, timeoutMs = 500))
    }
}
