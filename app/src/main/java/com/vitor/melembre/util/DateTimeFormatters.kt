package com.vitor.melembre.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormatters {
    private val localePtBr = Locale.forLanguageTag("pt-BR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", localePtBr)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", localePtBr)

    fun formatReminderSchedule(scheduledAtMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(scheduledAtMillis)
        val dateTime = instant.atZone(zoneId)
        val date = dateTime.toLocalDate()
        val time = timeFormatter.format(dateTime)
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        return when (date) {
            today -> "Hoje às $time"
            tomorrow -> "Amanhã às $time"
            else -> "${dateFormatter.format(date)} às $time"
        }
    }
}
