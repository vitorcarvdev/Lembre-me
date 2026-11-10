package com.vitor.melembre

import android.app.Application
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.notification.NotificationHelper
import com.vitor.melembre.repository.ReminderRepository
import androidx.core.content.pm.ShortcutManagerCompat

class MeLembreApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), database.taskListDao(), this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        // Atalho de voz desativado: remove o que versões anteriores publicaram.
        runCatching { ShortcutManagerCompat.removeAllDynamicShortcuts(this) }
    }
}
