package com.vitor.melembre.repository

import android.content.Context
import com.vitor.melembre.alarm.ReminderScheduler
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.ReminderDao
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.data.TaskListDao
import com.vitor.melembre.data.TaskListNames
import com.vitor.melembre.util.TaskListRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReminderRepository(
    private val dao: ReminderDao,
    private val taskListDao: TaskListDao,
    private val appContext: Context,
) {
    fun observeUpcoming(): Flow<List<Reminder>> = dao.observeUpcoming()

    fun observeHomeList(listId: Long? = null): Flow<List<Reminder>> {
        return if (listId == null) dao.observeHomeList() else dao.observeByList(listId)
    }

    fun observeLists(): Flow<List<TaskList>> = taskListDao.observeAll()

    suspend fun ensureDefaultLists() {
        val now = System.currentTimeMillis()
        TaskListNames.defaults.forEachIndexed { index, name ->
            if (taskListDao.findByNameInsensitive(name) == null) {
                taskListDao.insert(
                    TaskList(
                        name = name,
                        isDefault = true,
                        createdAt = now + index,
                    ),
                )
            }
        }
    }

    suspend fun remindersListId(): Long {
        ensureDefaultLists()
        return taskListDao.findByName(TaskListNames.REMINDERS)?.id
            ?: throw MissingListException()
    }

    suspend fun createList(rawName: String): Result<Long> {
        val name = TaskListRules.normalizeName(rawName)
        if (name.isEmpty()) return Result.failure(EmptyListNameException())
        if (taskListDao.findByNameInsensitive(name) != null) {
            return Result.failure(DuplicateListException())
        }
        val id = taskListDao.insert(
            TaskList(
                name = name,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return Result.success(id)
    }

    suspend fun renameList(id: Long, rawName: String): Result<Unit> {
        val list = taskListDao.getById(id) ?: return Result.failure(MissingListException())
        if (list.isDefault) return Result.failure(DefaultListProtectedException())
        val name = TaskListRules.normalizeName(rawName)
        if (name.isEmpty()) return Result.failure(EmptyListNameException())
        val existing = taskListDao.findByNameInsensitive(name)
        if (existing != null && existing.id != id) {
            return Result.failure(DuplicateListException())
        }
        taskListDao.update(list.copy(name = name))
        return Result.success(Unit)
    }

    suspend fun countInList(listId: Long): Int = taskListDao.countReminders(listId)

    suspend fun deleteCustomList(listId: Long): Result<Unit> {
        val list = taskListDao.getById(listId) ?: return Result.failure(MissingListException())
        if (list.isDefault) return Result.failure(DefaultListProtectedException())
        val destinationId = remindersListId()
        taskListDao.deleteMovingReminders(listId, destinationId)
        return Result.success(Unit)
    }

    suspend fun add(
        message: String,
        scheduledAt: Long?,
        recurrence: Recurrence,
        listId: Long?,
    ): Result<Long> {
        val resolvedListId = listId ?: remindersListId()
        if (taskListDao.getById(resolvedListId) == null) {
            return Result.failure(MissingListException())
        }
        if (scheduledAt != null && !ReminderScheduler.canScheduleExactAlarms(appContext)) {
            return Result.failure(ExactAlarmNotAllowedException())
        }
        val reminder = Reminder(
            message = message.trim(),
            scheduledAt = scheduledAt,
            recurrence = if (scheduledAt == null) Recurrence.NONE.name else recurrence.name,
            listId = resolvedListId,
        )
        val id = dao.insert(reminder)
        val saved = reminder.copy(id = id)
        if (scheduledAt != null) {
            ReminderScheduler.schedule(appContext, saved)
        }
        return Result.success(id)
    }

    suspend fun update(
        reminder: Reminder,
        message: String,
        scheduledAt: Long?,
        recurrence: Recurrence,
        listId: Long,
    ): Result<Unit> {
        if (taskListDao.getById(listId) == null) {
            return Result.failure(MissingListException())
        }
        if (scheduledAt != null && !ReminderScheduler.canScheduleExactAlarms(appContext)) {
            return Result.failure(ExactAlarmNotAllowedException())
        }
        ReminderScheduler.cancel(appContext, reminder.id)
        val updated = reminder.copy(
            message = message.trim(),
            scheduledAt = scheduledAt,
            triggered = false,
            recurrence = if (scheduledAt == null) Recurrence.NONE.name else recurrence.name,
            listId = listId,
        )
        dao.update(updated)
        if (scheduledAt != null) {
            ReminderScheduler.schedule(appContext, updated)
        }
        return Result.success(Unit)
    }

    suspend fun delete(reminder: Reminder) {
        ReminderScheduler.cancel(appContext, reminder.id)
        dao.delete(reminder)
    }

    suspend fun listHome(): List<Reminder> = dao.observeHomeList().first()

    suspend fun lists(): List<TaskList> = taskListDao.getAll()

    suspend fun findById(id: Long): Reminder? = dao.getById(id)

    suspend fun snoozeOneHour(reminder: Reminder): Result<Unit> {
        val current = reminder.scheduledAt ?: return Result.failure(NoDeadlineException())
        if (!ReminderScheduler.canScheduleExactAlarms(appContext)) {
            return Result.failure(ExactAlarmNotAllowedException())
        }
        val newTime = maxOf(System.currentTimeMillis(), current) + 60 * 60 * 1000L
        ReminderScheduler.cancel(appContext, reminder.id)
        val updated = reminder.copy(scheduledAt = newTime, triggered = false)
        dao.update(updated)
        ReminderScheduler.schedule(appContext, updated)
        return Result.success(Unit)
    }

    class ExactAlarmNotAllowedException : Exception()
    class NoDeadlineException : Exception()
    class EmptyListNameException : Exception()
    class DuplicateListException : Exception()
    class DefaultListProtectedException : Exception()
    class MissingListException : Exception()
}
