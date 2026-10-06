package com.vitor.melembre.util

import com.vitor.melembre.data.Reminder

object ReminderOrdering {
    fun isCompleted(reminder: Reminder, nowMillis: Long): Boolean {
        val scheduledAt = reminder.scheduledAt
        return reminder.triggered || (scheduledAt != null && scheduledAt <= nowMillis)
    }

    fun ordered(reminders: List<Reminder>, nowMillis: Long): List<Reminder> {
        return reminders.sortedWith(
            compareBy<Reminder> { group(it, nowMillis) }
                .thenBy { if (it.scheduledAt == null) 1 else 0 }
                .thenBy { it.scheduledAt ?: Long.MAX_VALUE }
                .thenBy { it.id },
        )
    }

    private fun group(reminder: Reminder, nowMillis: Long): Int {
        val scheduledAt = reminder.scheduledAt
        return when {
            !reminder.triggered && scheduledAt != null && scheduledAt > nowMillis -> 0
            !reminder.triggered && scheduledAt == null -> 1
            else -> 2
        }
    }
}
