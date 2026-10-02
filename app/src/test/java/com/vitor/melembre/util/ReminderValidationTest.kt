package com.vitor.melembre.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderValidationTest {
    @Test
    fun rejectsBlankMessage() {
        assertFalse(ReminderValidation.isMessageValid("   "))
    }

    @Test
    fun acceptsNonBlankMessage() {
        assertTrue(ReminderValidation.isMessageValid("Colocar o lixo para fora"))
    }

    @Test
    fun rejectsPastSchedule() {
        assertFalse(ReminderValidation.isScheduledInFuture(1_000L, nowMillis = 2_000L))
    }

    @Test
    fun acceptsFutureSchedule() {
        assertTrue(ReminderValidation.isScheduledInFuture(3_000L, nowMillis = 2_000L))
    }
}
