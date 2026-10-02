package com.vitor.melembre.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitor.melembre.MeLembreApp
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.repository.ReminderRepository
import com.vitor.melembre.util.ReminderValidation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository =
        (application as MeLembreApp).reminderRepository

    val upcomingReminders = repository.observeUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed class SaveResult {
        data object Success : SaveResult()
        data object EmptyMessage : SaveResult()
        data object PastDateTime : SaveResult()
        data object ExactAlarmDenied : SaveResult()
    }

    fun saveReminder(
        existing: Reminder?,
        message: String,
        scheduledAt: Long,
        onResult: (SaveResult) -> Unit,
    ) {
        if (!ReminderValidation.isMessageValid(message)) {
            onResult(SaveResult.EmptyMessage)
            return
        }
        if (!ReminderValidation.isScheduledInFuture(scheduledAt)) {
            onResult(SaveResult.PastDateTime)
            return
        }

        viewModelScope.launch {
            val result = if (existing == null) {
                repository.add(message, scheduledAt).map { Unit }
            } else {
                repository.update(existing, message, scheduledAt)
            }

            result.fold(
                onSuccess = { onResult(SaveResult.Success) },
                onFailure = { error ->
                    if (error is ReminderRepository.ExactAlarmNotAllowedException) {
                        onResult(SaveResult.ExactAlarmDenied)
                    } else {
                        onResult(SaveResult.PastDateTime)
                    }
                },
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.delete(reminder)
        }
    }
}
