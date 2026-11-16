package com.vitor.melembre.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.localweb.LocalAccessService
import com.vitor.melembre.util.RecurrenceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Depois de atualizar o app os alarmes continuam valendo; só o acesso pelo computador precisa voltar
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            LocalAccessService.startAuto(context)
            return
        }
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Reiniciou o celular: religa o acesso pelo computador se o usuário o deixou ligado
        LocalAccessService.startAuto(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val active = dao.getActiveForReschedule()
                val now = System.currentTimeMillis()

                active.forEach { reminder ->
                    val scheduledAt = reminder.scheduledAt ?: return@forEach
                    when {
                        scheduledAt > now -> {
                            ReminderScheduler.cancel(context, reminder.id)
                            ReminderScheduler.schedule(context, reminder)
                        }
                        reminder.recurrenceType == Recurrence.NONE -> {
                            dao.markTriggered(reminder.id)
                            ReminderScheduler.cancel(context, reminder.id)
                        }
                        else -> {
                            val nextAt = RecurrenceCalculator.nextOccurrence(
                                fromMillis = scheduledAt,
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
