package com.vitor.melembre.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["listId"])],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val scheduledAt: Long?,
    val triggered: Boolean = false,
    val recurrence: String = Recurrence.NONE.name,
    val listId: Long = 0,
) {
    val recurrenceType: Recurrence
        get() = Recurrence.fromStorage(recurrence)

    val hasDeadline: Boolean
        get() = scheduledAt != null
}
