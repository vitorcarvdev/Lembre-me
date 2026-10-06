package com.vitor.melembre.util

object TaskListRules {
    fun normalizeName(raw: String): String {
        return raw.trim().replace(Regex("\\s+"), " ")
    }

    fun isDuplicate(existingNames: List<String>, candidate: String): Boolean {
        val normalized = normalizeName(candidate)
        if (normalized.isEmpty()) return false
        return existingNames.any { it.equals(normalized, ignoreCase = true) }
    }
}
