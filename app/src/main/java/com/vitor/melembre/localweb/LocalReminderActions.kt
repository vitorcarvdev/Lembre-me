package com.vitor.melembre.localweb

import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.focus.FocusAgenda
import com.vitor.melembre.focus.FocusBlock
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.repository.ReminderRepository

interface LocalReminderActions {
    suspend fun lists(): List<TaskList>
    suspend fun list(): List<Reminder>
    suspend fun add(message: String, scheduledAt: Long?, listId: Long, url: String = ""): Result<Long>
    suspend fun update(id: Long, message: String, scheduledAt: Long?, listId: Long, url: String = ""): Result<Unit>
    suspend fun snoozeOneHour(id: Long): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun complete(id: Long): Result<Unit>
    suspend fun reopen(id: Long): Result<Unit>
    suspend fun reorder(listId: Long, orderedIds: List<Long>): Result<Unit>
    fun focusAgenda(): List<FocusBlock> = emptyList()
    fun saveFocusAgenda(blocks: List<FocusBlock>) {}
}

sealed class LocalAccessFailure : Exception() {
    class Missing : LocalAccessFailure()
    class ExactAlarm : LocalAccessFailure()
    class NoDeadline : LocalAccessFailure()
    class PastDeadline : LocalAccessFailure()
}

class RepositoryReminderActions(
    private val repository: ReminderRepository,
    private val focus: FocusAgenda? = null,
) : LocalReminderActions {
    override fun focusAgenda(): List<FocusBlock> = focus?.load().orEmpty()

    override fun saveFocusAgenda(blocks: List<FocusBlock>) {
        focus?.save(blocks)
    }

    override suspend fun lists(): List<TaskList> = repository.lists()

    override suspend fun list(): List<Reminder> = repository.listHome()

    override suspend fun add(message: String, scheduledAt: Long?, listId: Long, url: String): Result<Long> {
        return repository.add(message, scheduledAt, Recurrence.NONE, listId, url).mapAlarmFailure()
    }

    override suspend fun update(
        id: Long,
        message: String,
        scheduledAt: Long?,
        listId: Long,
        url: String,
    ): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        return repository.update(
            existing,
            message,
            scheduledAt,
            existing.recurrenceType,
            listId,
            url,
        ).mapAlarmFailure()
    }

    override suspend fun snoozeOneHour(id: Long): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        return repository.snoozeOneHour(existing).mapAlarmFailure()
    }

    override suspend fun delete(id: Long): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        repository.delete(existing)
        return Result.success(Unit)
    }

    override suspend fun complete(id: Long): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        repository.complete(existing)
        return Result.success(Unit)
    }

    override suspend fun reorder(listId: Long, orderedIds: List<Long>): Result<Unit> {
        repository.reorder(listId, orderedIds)
        return Result.success(Unit)
    }

    override suspend fun reopen(id: Long): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        return repository.reopen(existing).mapAlarmFailure()
    }

    private fun <T> Result<T>.mapAlarmFailure(): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                when (error) {
                    is ReminderRepository.ExactAlarmNotAllowedException -> Result.failure(LocalAccessFailure.ExactAlarm())
                    is ReminderRepository.NoDeadlineException -> Result.failure(LocalAccessFailure.NoDeadline())
                    is ReminderRepository.PastDeadlineException -> Result.failure(LocalAccessFailure.PastDeadline())
                    else -> Result.failure(error)
                }
            },
        )
    }
}
