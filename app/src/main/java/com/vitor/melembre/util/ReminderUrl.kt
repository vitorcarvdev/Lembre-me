package com.vitor.melembre.util

object ReminderUrl {
    // Esquema explícito (x://) ou esquemas perigosos sem "//"; "host:porta" não conta.
    private val scheme = Regex(
        "^([a-zA-Z][a-zA-Z0-9+.-]*)://|^(javascript|data|vbscript|file|blob|mailto|tel):",
        RegexOption.IGNORE_CASE,
    )
    private const val MAX_LENGTH = 2000

    /** Retorna "" para vazio, a URL com http/https, ou null se for inválida. */
    fun normalize(raw: String?): String? {
        val value = raw.orEmpty().trim()
        if (value.isEmpty()) return ""
        if (value.length > MAX_LENGTH || value.any { it.isWhitespace() }) return null
        val match = scheme.find(value)
        val found = match?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }?.lowercase()
        val full = when {
            found == null -> "https://$value"
            found == "http" || found == "https" -> value
            else -> return null
        }
        val rest = full.substringAfter("://")
        return if (rest.isEmpty() || rest.startsWith("/")) null else full
    }
}
