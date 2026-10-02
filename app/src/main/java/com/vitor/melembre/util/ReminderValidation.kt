package com.vitor.melembre.util

object ReminderValidation {
    fun isMessageValid(message: String): Boolean = message.trim().isNotEmpty()

    fun isScheduledInFuture(scheduledAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return scheduledAtMillis > nowMillis
    }
}
