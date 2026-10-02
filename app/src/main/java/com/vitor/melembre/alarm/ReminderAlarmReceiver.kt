package com.vitor.melembre.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.notification.NotificationHelper
import com.vitor.melembre.util.RecurrenceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val reminder = dao.getById(reminderId) ?: return@launch
                if (reminder.triggered) {
                    return@launch
                }

                NotificationHelper.showReminderNotification(context, reminder.id, reminder.message)

                val recurrence = reminder.recurrenceType
                if (recurrence == Recurrence.NONE) {
                    dao.markTriggered(reminder.id)
                    ReminderScheduler.cancel(context, reminder.id)
                } else {
                    val nextAt = RecurrenceCalculator.nextOccurrence(
                        fromMillis = reminder.scheduledAt,
                        recurrence = recurrence,
                    )
                    if (nextAt == null) {
                        dao.markTriggered(reminder.id)
                        ReminderScheduler.cancel(context, reminder.id)
                    } else {
                        val updated = reminder.copy(scheduledAt = nextAt, triggered = false)
                        dao.update(updated)
                        ReminderScheduler.cancel(context, reminder.id)
                        ReminderScheduler.schedule(context, updated)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
