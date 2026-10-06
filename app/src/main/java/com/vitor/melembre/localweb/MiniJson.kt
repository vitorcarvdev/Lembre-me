package com.vitor.melembre.localweb

object MiniJson {
    fun escape(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u%04x".format(ch.code))
                } else {
                    append(ch)
                }
            }
        }
    }

    fun parseFlatObject(raw: String): Map<String, String>? {
        val text = raw.trim()
        if (text.length < 2 || text.first() != '{' || text.last() != '}') return null
        val inner = text.substring(1, text.lastIndex)
        var index = 0

        fun skipWs() {
            while (index < inner.length && inner[index].isWhitespace()) index++
        }

        fun readString(): String? {
            if (index >= inner.length || inner[index] != '"') return null
            index++
            val out = StringBuilder()
            while (index < inner.length) {
                val ch = inner[index]
                when {
                    ch == '"' -> {
                        index++
                        return out.toString()
                    }
                    ch == '\\' -> {
                        index++
                        if (index >= inner.length) return null
                        when (val escaped = inner[index]) {
                            '"', '\\', '/' -> out.append(escaped)
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 >= inner.length) return null
                                val hex = inner.substring(index + 1, index + 5)
                                val code = hex.toIntOrNull(16) ?: return null
                                out.append(code.toChar())
                                index += 4
                            }
                            else -> return null
                        }
                        index++
                    }
                    else -> {
                        out.append(ch)
                        index++
                    }
                }
            }
            return null
        }

        val result = linkedMapOf<String, String>()
        skipWs()
        if (index >= inner.length) return result
        while (index < inner.length) {
            skipWs()
            val key = readString() ?: return null
            skipWs()
            if (index >= inner.length || inner[index] != ':') return null
            index++
            skipWs()
            if (index >= inner.length) return null
            val value = if (inner[index] == '"') {
                readString() ?: return null
            } else {
                val start = index
                while (index < inner.length && inner[index] != ',' && !inner[index].isWhitespace()) {
                    index++
                }
                inner.substring(start, index)
            }
            result[key] = value
            skipWs()
            if (index >= inner.length) break
            if (inner[index] != ',') return null
            index++
        }
        return result
    }
}
