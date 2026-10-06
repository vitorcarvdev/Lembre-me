package com.vitor.melembre.localweb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object LocalIp {
    fun wifiIpv4(context: Context): String? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val addresses = manager.getLinkProperties(network)
            ?.linkAddresses
            ?.map { it.address.hostAddress }
            .orEmpty()
        return pickLanIpv4(addresses)
    }

    fun pickLanIpv4(addresses: List<String?>): String? {
        return addresses.firstOrNull { address -> address != null && isUsableLanIpv4(address) }
    }

    fun isUsableLanIpv4(ip: String): Boolean {
        if (ip.contains(':')) return false
        val parts = ip.split('.')
        if (parts.size != 4) return false
        val numbers = parts.map { part ->
            if (part.length > 1 && part.startsWith('0')) return false
            part.toIntOrNull() ?: return false
        }
        if (numbers.any { it !in 0..255 }) return false
        val first = numbers[0]
        val second = numbers[1]
        if (first == 0 || first >= 224) return false
        if (first == 127) return false
        if (first == 169 && second == 254) return false
        return true
    }
}
