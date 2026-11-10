package com.vitor.melembre.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vitor.melembre.repository.ReminderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReorderRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private suspend fun names(dao: ReminderDao, listId: Long): List<String> =
        dao.observeByList(listId).first().map { it.message }

    @Test
    fun reorderKeepsListsIndependentAndSurvivesCompleteReopen() = runBlocking {
        val database = AppDatabase.inMemory(context, allowMainThread = true)
        val repository = ReminderRepository(database.reminderDao(), database.taskListDao(), context)
        val dao = database.reminderDao()
        try {
            val inbox = repository.remindersListId()
            val other = repository.createList("Casa").getOrThrow()
            val a = repository.add("A", null, Recurrence.NONE, inbox).getOrThrow()
            val b = repository.add("B", null, Recurrence.NONE, inbox).getOrThrow()
            val c = repository.add("C", null, Recurrence.NONE, inbox).getOrThrow()
            repository.add("X", null, Recurrence.NONE, other).getOrThrow()
            repository.add("Y", null, Recurrence.NONE, other).getOrThrow()
            val future = System.currentTimeMillis() + 3 * 60 * 60 * 1000L
            repository.add("Com prazo", future, Recurrence.NONE, inbox).getOrThrow()

            assertEquals(listOf("Com prazo", "A", "B", "C"), names(dao, inbox))

            repository.reorder(inbox, listOf(c, a, b))
            assertEquals(listOf("Com prazo", "C", "A", "B"), names(dao, inbox))
            assertEquals(listOf("X", "Y"), names(dao, other))

            // ids de outra lista são ignorados
            val xId = dao.observeByList(other).first().first().id
            repository.reorder(inbox, listOf(xId, c))
            assertEquals(listOf("Com prazo", "C", "A", "B"), names(dao, inbox))

            // concluir vai para o fim; reabrir devolve à posição manual
            repository.complete(dao.getById(a)!!)
            assertEquals(listOf("Com prazo", "C", "B", "A"), names(dao, inbox))
            repository.reopen(dao.getById(a)!!).getOrThrow()
            assertEquals(listOf("Com prazo", "C", "A", "B"), names(dao, inbox))

            // tarefa movida de lista vai para o fim da nova lista
            repository.update(dao.getById(c)!!, "C", null, Recurrence.NONE, other).getOrThrow()
            assertEquals(listOf("X", "Y", "C"), names(dao, other))
        } finally {
            database.close()
        }
    }

    @Test
    fun orderPersistsAfterReopeningDatabase() = runBlocking {
        val name = "reorder-persist.db"
        context.deleteDatabase(name)
        var database = AppDatabase.build(context, name, allowMainThread = true)
        val inbox: Long
        try {
            val repository = ReminderRepository(database.reminderDao(), database.taskListDao(), context)
            inbox = repository.remindersListId()
            val one = repository.add("Um", null, Recurrence.NONE, inbox).getOrThrow()
            val two = repository.add("Dois", null, Recurrence.NONE, inbox).getOrThrow()
            repository.reorder(inbox, listOf(two, one))
        } finally {
            database.close()
        }
        database = AppDatabase.build(context, name, allowMainThread = true)
        try {
            assertEquals(listOf("Dois", "Um"), names(database.reminderDao(), inbox))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
