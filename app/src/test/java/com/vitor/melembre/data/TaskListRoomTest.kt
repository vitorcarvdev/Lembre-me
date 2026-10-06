package com.vitor.melembre.data

import android.app.AlarmManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.vitor.melembre.repository.ReminderRepository
import com.vitor.melembre.util.ReminderOrdering
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaskListRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun migrationCreatesDefaultsAndKeepsOldReminders() = runBlocking {
        val name = "legacy-reminders.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        legacy.execSQL(
            """
            CREATE TABLE reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                message TEXT NOT NULL,
                scheduledAt INTEGER NOT NULL,
                triggered INTEGER NOT NULL,
                recurrence TEXT NOT NULL
            )
            """.trimIndent(),
        )
        legacy.execSQL(
            """
            CREATE TABLE room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
            """.trimIndent(),
        )
        legacy.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'legacy-v2')")
        legacy.execSQL(
            "INSERT INTO reminders (id, message, scheduledAt, triggered, recurrence) VALUES (7, 'Antigo', 1700000000000, 1, 'NONE')",
        )
        legacy.execSQL(
            "INSERT INTO reminders (id, message, scheduledAt, triggered, recurrence) VALUES (8, 'Futuro', 1800000000000, 0, 'DAILY')",
        )
        legacy.version = 2
        legacy.close()

        val database = AppDatabase.build(context, name, allowMainThread = true)
        try {
            val lists = database.taskListDao().getAll()
            assertEquals(listOf("Lembretes", "Compras", "Trabalho", "Ideias"), lists.map { it.name })
            assertTrue(lists.all { it.isDefault })

            val oldDone = database.reminderDao().getById(7)
            val oldOpen = database.reminderDao().getById(8)
            val remindersId = lists.first { it.name == TaskListNames.REMINDERS }.id
            assertEquals("Antigo", oldDone?.message)
            assertEquals(1700000000000L, oldDone?.scheduledAt)
            assertEquals(true, oldDone?.triggered)
            assertEquals("NONE", oldDone?.recurrence)
            assertEquals(remindersId, oldDone?.listId)
            assertEquals("Futuro", oldOpen?.message)
            assertEquals(1800000000000L, oldOpen?.scheduledAt)
            assertEquals(false, oldOpen?.triggered)
            assertEquals("DAILY", oldOpen?.recurrence)
            assertEquals(remindersId, oldOpen?.listId)
            assertEquals(2, database.reminderDao().getActiveForReschedule().size + if (oldDone?.triggered == true) 1 else 0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun freshDatabaseSeedsFourDefaultLists() = runBlocking {
        val database = AppDatabase.inMemory(context, allowMainThread = true)
        try {
            val lists = database.taskListDao().getAll()
            assertEquals(TaskListNames.defaults, lists.map { it.name })
            assertTrue(lists.all { it.isDefault })
            assertTrue(database.reminderDao().getUpcoming().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun customListsAndReminderDeadlines() = runBlocking {
        val database = AppDatabase.inMemory(context, allowMainThread = true)
        val repository = ReminderRepository(
            database.reminderDao(),
            database.taskListDao(),
            context,
        )
        try {
            val remindersId = repository.remindersListId()
            val shopping = repository.createList("Compras").exceptionOrNull()
            assertTrue(shopping is ReminderRepository.DuplicateListException)
            val home = repository.createList("  Casa   nova ").getOrThrow()
            assertTrue(repository.createList("casa nova").isFailure)
            assertTrue(repository.createList("   ").isFailure)
            assertEquals("Casa nova", database.taskListDao().getById(home)?.name)

            repository.renameList(home, "Carro").getOrThrow()
            assertEquals("Carro", database.taskListDao().getById(home)?.name)
            assertTrue(repository.renameList(remindersId, "Outro").exceptionOrNull() is ReminderRepository.DefaultListProtectedException)

            val future = System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            val datedId = repository.add("Com prazo", future, Recurrence.NONE, home).getOrThrow()
            val undatedId = repository.add("Sem prazo", null, Recurrence.DAILY, null).getOrThrow()
            val dated = database.reminderDao().getById(datedId)!!
            val undated = database.reminderDao().getById(undatedId)!!
            assertEquals(home, dated.listId)
            assertEquals(future, dated.scheduledAt)
            assertEquals(1, scheduledAlarmCount())
            assertNull(undated.scheduledAt)
            assertEquals(Recurrence.NONE.name, undated.recurrence)
            assertEquals(remindersId, undated.listId)
            assertFalse(ReminderOrdering.isCompleted(undated, System.currentTimeMillis()))
            assertTrue(repository.snoozeOneHour(undated).exceptionOrNull() is ReminderRepository.NoDeadlineException)
            assertNull(database.reminderDao().getById(undatedId)?.scheduledAt)

            repository.update(dated, "Com prazo", null, Recurrence.NONE, home).getOrThrow()
            assertNull(database.reminderDao().getById(datedId)?.scheduledAt)
            assertEquals(0, scheduledAlarmCount())

            val newTime = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
            val moved = database.reminderDao().getById(datedId)!!
            repository.update(moved, "No trabalho", newTime, Recurrence.NONE, remindersId).getOrThrow()
            val withDeadline = database.reminderDao().getById(datedId)!!
            assertEquals(remindersId, withDeadline.listId)
            assertEquals(newTime, withDeadline.scheduledAt)
            assertEquals(1, scheduledAlarmCount())

            repository.snoozeOneHour(withDeadline).getOrThrow()
            assertEquals(newTime + 60 * 60 * 1000L, database.reminderDao().getById(datedId)?.scheduledAt)

            database.reminderDao().markTriggered(undatedId)
            val completed = database.reminderDao().getById(undatedId)!!
            assertTrue(completed.triggered)
            assertTrue(ReminderOrdering.isCompleted(completed, System.currentTimeMillis()))
            assertEquals(remindersId, completed.listId)

            repository.delete(database.reminderDao().getById(datedId)!!)
            assertNull(database.reminderDao().getById(datedId))
            assertEquals(0, scheduledAlarmCount())

            val emptyCustom = repository.createList("Viagem").getOrThrow()
            repository.deleteCustomList(emptyCustom).getOrThrow()
            assertNull(database.taskListDao().getById(emptyCustom))

            val clients = repository.createList("Clientes").getOrThrow()
            repository.add("Ligar", null, Recurrence.NONE, clients).getOrThrow()
            repository.add("Visitar", future, Recurrence.NONE, clients).getOrThrow()
            assertEquals(2, repository.countInList(clients))
            repository.deleteCustomList(clients).getOrThrow()
            assertNull(database.taskListDao().getById(clients))
            val movedItems = database.reminderDao().observeByList(remindersId).first()
            assertTrue(movedItems.any { it.message == "Ligar" && it.listId == remindersId })
            assertTrue(movedItems.any { it.message == "Visitar" && it.listId == remindersId })
            assertTrue(repository.deleteCustomList(remindersId).exceptionOrNull() is ReminderRepository.DefaultListProtectedException)
        } finally {
            database.close()
        }
    }

    private fun scheduledAlarmCount(): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Shadows.shadowOf(alarmManager).scheduledAlarms.size
    }
}
