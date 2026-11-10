package com.vitor.melembre.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitor.melembre.MeLembreApp
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.data.TaskListNames
import com.vitor.melembre.repository.ReminderRepository
import com.vitor.melembre.util.ReminderOrdering
import com.vitor.melembre.util.ReminderValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository =
        (application as MeLembreApp).reminderRepository

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId = _selectedListId.asStateFlow()
    private val _filterReady = MutableStateFlow(false)
    val filterReady = _filterReady.asStateFlow()
    private var selectionTouched = false

    val taskLists = repository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeReminders = combine(_filterReady, _selectedListId) { ready, listId ->
        ready to listId
    }.flatMapLatest { (ready, listId) ->
        if (!ready) flowOf(emptyList()) else repository.observeHomeList(listId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tarefas ainda abertas por lista (para os números do menu lateral). */
    val openCounts = repository.observeHomeList(null)
        .map { all ->
            val now = System.currentTimeMillis()
            all.filter { !ReminderOrdering.isCompleted(it, now) }
                .groupingBy { it.listId }
                .eachCount()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            repository.ensureDefaultLists()
            if (!selectionTouched) {
                _selectedListId.value = repository.remindersListId()
            }
            _filterReady.value = true
        }
    }

    fun selectList(listId: Long?) {
        selectionTouched = true
        _selectedListId.value = listId
    }

    fun listIdForNewItem(): Long? {
        _selectedListId.value?.let { return it }
        return taskLists.value.firstOrNull { it.name == TaskListNames.REMINDERS }?.id
    }

    sealed class SaveResult {
        data object Success : SaveResult()
        data object EmptyMessage : SaveResult()
        data object PastDateTime : SaveResult()
        data object ExactAlarmDenied : SaveResult()
        data object MissingList : SaveResult()
    }

    sealed class ListSaveResult {
        data class Success(val id: Long = 0) : ListSaveResult()
        data object EmptyName : ListSaveResult()
        data object Duplicate : ListSaveResult()
        data object Protected : ListSaveResult()
        data object Missing : ListSaveResult()
    }

    fun saveReminder(
        existing: Reminder?,
        message: String,
        listId: Long?,
        scheduledAt: Long?,
        recurrence: Recurrence,
        url: String? = null,
        onResult: (SaveResult) -> Unit,
    ) {
        if (!ReminderValidation.isMessageValid(message)) {
            onResult(SaveResult.EmptyMessage)
            return
        }
        if (scheduledAt != null && !ReminderValidation.isScheduledInFuture(scheduledAt)) {
            onResult(SaveResult.PastDateTime)
            return
        }

        viewModelScope.launch {
            val result = if (existing == null) {
                repository.add(message, scheduledAt, recurrence, listId, url.orEmpty()).map { Unit }
            } else {
                val resolvedListId = listId ?: existing.listId
                repository.update(existing, message, scheduledAt, recurrence, resolvedListId, url ?: existing.url)
            }

            result.fold(
                onSuccess = { onResult(SaveResult.Success) },
                onFailure = { error ->
                    onResult(
                        when (error) {
                            is ReminderRepository.ExactAlarmNotAllowedException -> SaveResult.ExactAlarmDenied
                            is ReminderRepository.MissingListException -> SaveResult.MissingList
                            else -> SaveResult.PastDateTime
                        },
                    )
                },
            )
        }
    }

    fun createList(name: String, onResult: (ListSaveResult) -> Unit) {
        viewModelScope.launch {
            onResult(repository.createList(name).toListResult())
        }
    }

    fun renameList(id: Long, name: String, onResult: (ListSaveResult) -> Unit) {
        viewModelScope.launch {
            onResult(repository.renameList(id, name).toListResult())
        }
    }

    fun deleteList(id: Long, onResult: (ListSaveResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteCustomList(id)
            if (result.isSuccess && _selectedListId.value == id) {
                _selectedListId.value = repository.remindersListId()
            }
            onResult(result.toListResult())
        }
    }

    fun countTasks(listId: Long, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            onResult(repository.countInList(listId))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.delete(reminder)
        }
    }

    fun reorder(listId: Long, orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorder(listId, orderedIds) }
    }

    fun toggleCompleted(reminder: Reminder, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val reminder = repository.findById(reminder.id) ?: return@launch
            if (ReminderOrdering.isCompleted(reminder, System.currentTimeMillis())) {
                repository.reopen(reminder).onFailure { error ->
                    onError(
                        when (error) {
                            is ReminderRepository.PastDeadlineException ->
                                "O prazo já passou. Edite a data para reabrir."
                            is ReminderRepository.ExactAlarmNotAllowedException ->
                                "Ative alarmes exatos para reabrir o lembrete."
                            else -> "Não foi possível reabrir."
                        },
                    )
                }
            } else {
                repository.complete(reminder)
            }
        }
    }

    fun snoozeReminderOneHour(reminder: Reminder, onDenied: () -> Unit = {}) {
        if (reminder.scheduledAt == null) return
        viewModelScope.launch {
            repository.snoozeOneHour(reminder).onFailure { error ->
                if (error is ReminderRepository.ExactAlarmNotAllowedException) {
                    onDenied()
                }
            }
        }
    }

    private fun Result<*>.toListResult(): ListSaveResult {
        return fold(
            onSuccess = { value ->
                ListSaveResult.Success(id = (value as? Long) ?: 0L)
            },
            onFailure = { error ->
                when (error) {
                    is ReminderRepository.EmptyListNameException -> ListSaveResult.EmptyName
                    is ReminderRepository.DuplicateListException -> ListSaveResult.Duplicate
                    is ReminderRepository.DefaultListProtectedException -> ListSaveResult.Protected
                    else -> ListSaveResult.Missing
                }
            },
        )
    }
}

fun defaultListId(lists: List<TaskList>, selectedListId: Long?): Long? {
    if (selectedListId != null && lists.any { it.id == selectedListId }) return selectedListId
    return lists.firstOrNull { it.name == TaskListNames.REMINDERS }?.id
}
