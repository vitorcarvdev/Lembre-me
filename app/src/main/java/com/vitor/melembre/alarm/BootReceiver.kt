package com.vitor.melembre.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitor.melembre.data.AppDatabase
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
                val upcoming = dao.getUpcoming()
                ReminderScheduler.rescheduleAll(context, upcoming)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
