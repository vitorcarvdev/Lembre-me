package com.vitor.melembre.localweb

import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.repository.ReminderRepository

interface LocalReminderActions {
    suspend fun lists(): List<TaskList>
    suspend fun list(): List<Reminder>
    suspend fun add(message: String, scheduledAt: Long?, listId: Long): Result<Long>
    suspend fun update(id: Long, message: String, scheduledAt: Long?, listId: Long): Result<Unit>
    suspend fun snoozeOneHour(id: Long): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
}

sealed class LocalAccessFailure : Exception() {
    class Missing : LocalAccessFailure()
    class ExactAlarm : LocalAccessFailure()
    class NoDeadline : LocalAccessFailure()
}

class RepositoryReminderActions(
    private val repository: ReminderRepository,
) : LocalReminderActions {
    override suspend fun lists(): List<TaskList> = repository.lists()

    override suspend fun list(): List<Reminder> = repository.listHome()

    override suspend fun add(message: String, scheduledAt: Long?, listId: Long): Result<Long> {
        return repository.add(message, scheduledAt, Recurrence.NONE, listId).mapAlarmFailure()
    }

    override suspend fun update(
        id: Long,
        message: String,
        scheduledAt: Long?,
        listId: Long,
    ): Result<Unit> {
        val existing = repository.findById(id) ?: return Result.failure(LocalAccessFailure.Missing())
        return repository.update(
            existing,
            message,
            scheduledAt,
            existing.recurrenceType,
            listId,
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

    private fun <T> Result<T>.mapAlarmFailure(): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                when (error) {
                    is ReminderRepository.ExactAlarmNotAllowedException -> Result.failure(LocalAccessFailure.ExactAlarm())
                    is ReminderRepository.NoDeadlineException -> Result.failure(LocalAccessFailure.NoDeadline())
                    else -> Result.failure(error)
                }
            },
        )
    }
}
