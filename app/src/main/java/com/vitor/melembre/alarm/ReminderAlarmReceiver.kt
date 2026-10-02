package com.vitor.melembre.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.notification.NotificationHelper
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
                dao.markTriggered(reminder.id)
                ReminderScheduler.cancel(context, reminder.id)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
