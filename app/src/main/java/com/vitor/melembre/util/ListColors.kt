package com.vitor.melembre.util

object ListColors {
    private val assigned = listOf(
        "lembretes" to 0x6C3CE9,
        "compras" to 0x1F8A5B,
        "trabalho" to 0x2F6FBE,
        "ideias" to 0xC47A22,
    )

    private val extra = intArrayOf(
        0x8E4A8A,
        0x1F8A8A,
        0xB55268,
        0x6A7A32,
        0x5C6BC0,
        0xA15C38,
    )

    fun rgb(name: String): Int {
        val key = name.trim().lowercase()
        assigned.firstOrNull { it.first == key }?.let { return it.second }
        return extra[key.hashCode().mod(extra.size)]
    }

    fun onDark(name: String): Int = lighten(rgb(name))

    fun hex(name: String): String = "#%06X".format(rgb(name))

    private fun lighten(rgb: Int): Int {
        fun mix(channel: Int) = channel + ((255 - channel) * 0.38f).toInt()
        val red = mix((rgb shr 16) and 0xFF)
        val green = mix((rgb shr 8) and 0xFF)
        val blue = mix(rgb and 0xFF)
        return (red shl 16) or (green shl 8) or blue
    }
}
