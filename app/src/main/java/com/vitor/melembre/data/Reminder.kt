package com.vitor.melembre.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val scheduledAt: Long,
    val triggered: Boolean = false,
    val recurrence: String = Recurrence.NONE.name,
) {
    val recurrenceType: Recurrence
        get() = Recurrence.fromStorage(recurrence)
}
