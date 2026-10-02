package com.vitor.melembre.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DateTimeFormattersTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun formatsTodayLabel() {
        val today = LocalDate.now(zone)
        val millis = today.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant().toEpochMilli()
        val formatted = DateTimeFormatters.formatReminderSchedule(millis, zone)
        assertTrue(formatted.startsWith("Hoje às 17:00"))
    }
}
