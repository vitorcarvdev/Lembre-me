package com.vitor.melembre.util

import com.vitor.melembre.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceCalculatorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun noneReturnsNull() {
        assertNull(
            RecurrenceCalculator.nextOccurrence(
                fromMillis = 1_000L,
                recurrence = Recurrence.NONE,
                nowMillis = 2_000L,
                zoneId = zone,
            ),
        )
    }

    @Test
    fun dailyAdvancesOneDay() {
        val from = LocalDateTime.of(2026, 10, 2, 17, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2026, 10, 2, 17, 1).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(from, Recurrence.DAILY, now, zone)!!
        val nextLocal = java.time.Instant.ofEpochMilli(next).atZone(zone).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 10, 3, 17, 0), nextLocal)
    }

    @Test
    fun weeklySkipsUntilFuture() {
        val from = LocalDateTime.of(2026, 10, 2, 8, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2026, 10, 20, 9, 0).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(from, Recurrence.WEEKLY, now, zone)!!
        assertTrue(next > now)
        val nextLocal = java.time.Instant.ofEpochMilli(next).atZone(zone).toLocalDateTime()
        assertEquals(8, nextLocal.hour)
        assertEquals(0, nextLocal.minute)
    }
}
