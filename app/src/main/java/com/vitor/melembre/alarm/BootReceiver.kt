package com.vitor.melembre.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.util.RecurrenceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val active = dao.getActiveForReschedule()
                val now = System.currentTimeMillis()

                active.forEach { reminder ->
                    when {
                        reminder.scheduledAt > now -> {
                            ReminderScheduler.cancel(context, reminder.id)
                            ReminderScheduler.schedule(context, reminder)
                        }
                        reminder.recurrenceType == Recurrence.NONE -> {
                            dao.markTriggered(reminder.id)
                            ReminderScheduler.cancel(context, reminder.id)
                        }
                        else -> {
                            val nextAt = RecurrenceCalculator.nextOccurrence(
                                fromMillis = reminder.scheduledAt,
                                recurrence = reminder.recurrenceType,
                                nowMillis = now,
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
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
