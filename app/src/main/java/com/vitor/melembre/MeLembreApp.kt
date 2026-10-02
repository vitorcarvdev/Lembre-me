package com.vitor.melembre

import android.app.Application
import com.vitor.melembre.data.AppDatabase
import com.vitor.melembre.notification.NotificationHelper
import com.vitor.melembre.repository.ReminderRepository

class MeLembreApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
