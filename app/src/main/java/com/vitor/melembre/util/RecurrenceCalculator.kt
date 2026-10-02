package com.vitor.melembre.util

import com.vitor.melembre.data.Recurrence
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object RecurrenceCalculator {
    /**
     * Calcula a próxima ocorrência após [fromMillis], avançando até ficar no futuro
     * em relação a [nowMillis].
     */
    fun nextOccurrence(
        fromMillis: Long,
        recurrence: Recurrence,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (recurrence == Recurrence.NONE) return null

        var current = Instant.ofEpochMilli(fromMillis).atZone(zoneId)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)

        // Garante avanço pelo menos uma vez a partir do horário disparado.
        current = advance(current, recurrence)

        var guard = 0
        while (!current.toInstant().isAfter(now.toInstant()) && guard < 10_000) {
            current = advance(current, recurrence)
            guard++
        }

        return current.toInstant().toEpochMilli()
    }

    private fun advance(dateTime: ZonedDateTime, recurrence: Recurrence): ZonedDateTime {
        return when (recurrence) {
            Recurrence.NONE -> dateTime
            Recurrence.DAILY -> dateTime.plusDays(1)
            Recurrence.WEEKLY -> dateTime.plusWeeks(1)
            Recurrence.MONTHLY -> dateTime.plusMonths(1)
            Recurrence.YEARLY -> dateTime.plusYears(1)
        }
    }
}
