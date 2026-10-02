package com.vitor.melembre.repository

import android.content.Context
import com.vitor.melembre.alarm.ReminderScheduler
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.ReminderDao
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val dao: ReminderDao,
    private val appContext: Context,
) {
    fun observeUpcoming(): Flow<List<Reminder>> = dao.observeUpcoming()

    suspend fun add(message: String, scheduledAt: Long, recurrence: Recurrence): Result<Long> {
        if (!ReminderScheduler.canScheduleExactAlarms(appContext)) {
            return Result.failure(ExactAlarmNotAllowedException())
        }
        val reminder = Reminder(
            message = message.trim(),
            scheduledAt = scheduledAt,
            recurrence = recurrence.name,
        )
        val id = dao.insert(reminder)
        val saved = reminder.copy(id = id)
        ReminderScheduler.schedule(appContext, saved)
        return Result.success(id)
    }

    suspend fun update(
        reminder: Reminder,
        message: String,
        scheduledAt: Long,
        recurrence: Recurrence,
    ): Result<Unit> {
        if (!ReminderScheduler.canScheduleExactAlarms(appContext)) {
            return Result.failure(ExactAlarmNotAllowedException())
        }
        ReminderScheduler.cancel(appContext, reminder.id)
        val updated = reminder.copy(
            message = message.trim(),
            scheduledAt = scheduledAt,
            triggered = false,
            recurrence = recurrence.name,
        )
        dao.update(updated)
        ReminderScheduler.schedule(appContext, updated)
        return Result.success(Unit)
    }

    suspend fun delete(reminder: Reminder) {
        ReminderScheduler.cancel(appContext, reminder.id)
        dao.delete(reminder)
    }

    class ExactAlarmNotAllowedException : Exception()
}
