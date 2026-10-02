package com.vitor.melembre.voice

import com.vitor.melembre.data.Recurrence
import java.time.LocalDate
import java.time.LocalTime

data class ParsedVoiceReminder(
    val message: String,
    val date: LocalDate,
    val time: LocalTime,
    val recurrence: Recurrence = Recurrence.NONE,
    val rawText: String,
)
