package com.vitor.melembre.localweb

import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class LocalAccessServerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val nowMillis = at(2026, 10, 5, 12, 0)
    private lateinit var memory: MemoryReminders
    private var server: LocalAccessServer? = null
    private var port: Int = 0

    @Before
    fun setUp() {
        memory = MemoryReminders()
        memory.items += Reminder(
            id = 50,
            message = "Levar documentos",
            scheduledAt = at(2026, 10, 4, 10, 0),
            triggered = true,
        )
        server = startServer()
    }

    @After
    fun tearDown() {
        server?.stop()
    }

    @Test
    fun pageAsksForPinAndListsActions() {
        val (code, body) = exchange("GET", "/")
        assertEquals(200, code)
        assertTrue(body.contains("Lembre-me"))
        assertTrue(body.contains("PIN"))
        assertTrue(body.contains("+ Novo lembrete"))
        assertTrue(body.contains("Adiar 1h"))
        assertTrue(body.contains("Excluir"))
        assertTrue(body.contains("line-through"))
        assertTrue(body.contains("Todas"))
        assertTrue(body.contains("Sem prazo"))
        assertTrue(body.contains("Lista"))
    }

    @Test
    fun pinRejectsThenAccepts() {
        val wrong = exchange("POST", "/api/session", """{"pin":"0000"}""")
        assertEquals(401, wrong.first)
        assertFalse(wrong.second.contains("\"token\""))

        val right = exchange("POST", "/api/session", """{"pin":"4821"}""")
        assertEquals(200, right.first)
        assertNotNull(tokenFrom(right.second))
    }

    @Test
    fun remindersRequireSession() {
        val (code, _) = exchange("GET", "/api/reminders")
        assertEquals(401, code)
    }

    @Test
    fun createEditSnoozeAndDelete() {
        val token = unlock()
        val created = exchange(
            "POST",
            "/api/reminders",
            """{"message":"Comprar pão","listId":1,"date":"2026-10-06","time":"09:00"}""",
            token,
        )
        assertEquals(200, created.first)

        val listed = exchange("GET", "/api/reminders", token = token).second
        assertTrue(listed.indexOf("Comprar pão") < listed.indexOf("Levar documentos"))
        assertTrue(listed.contains("Amanhã 09:00"))
        assertTrue(listed.contains("Ontem 10:00"))
        assertTrue(listed.contains("\"completed\":true"))

        val edited = exchange(
            "PUT",
            "/api/reminders/1",
            """{"message":"Pagar internet","listId":1,"date":"2026-10-06","time":"14:00"}""",
            token,
        )
        assertEquals(200, edited.first)
        val afterEdit = exchange("GET", "/api/reminders", token = token).second
        assertTrue(afterEdit.contains("Pagar internet"))
        assertTrue(afterEdit.contains("Amanhã 14:00"))
        assertFalse(afterEdit.contains("Comprar pão"))

        val snoozed = exchange("POST", "/api/reminders/1/snooze", token = token)
        assertEquals(200, snoozed.first)
        val afterSnooze = exchange("GET", "/api/reminders", token = token).second
        assertTrue(afterSnooze.contains("\"time\":\"15:00\""))

        val deleted = exchange("DELETE", "/api/reminders/1", token = token)
        assertEquals(200, deleted.first)
        val afterDelete = exchange("GET", "/api/reminders", token = token).second
        assertFalse(afterDelete.contains("Pagar internet"))
        assertTrue(afterDelete.contains("Levar documentos"))
    }

    @Test
    fun rejectsEmptyMessageAndPastDate() {
        val token = unlock()
        val before = memory.items.size
        val empty = exchange(
            "POST",
            "/api/reminders",
            """{"message":"  ","listId":1,"date":"2026-10-06","time":"09:00"}""",
            token,
        )
        assertEquals(400, empty.first)
        assertTrue(empty.second.contains("Digite uma mensagem."))

        val past = exchange(
            "POST",
            "/api/reminders",
            """{"message":"Atrasado","listId":1,"date":"2026-10-05","time":"08:00"}""",
            token,
        )
        assertEquals(400, past.first)
        assertTrue(past.second.contains("Escolha uma data e horário futuros."))
        assertEquals(before, memory.items.size)
    }

    @Test
    fun filtersListsAndKeepsUndatedTasks() {
        val token = unlock()
        val shopping = exchange(
            "POST",
            "/api/reminders",
            """{"message":"Comprar café","listId":2,"date":"","time":""}""",
            token,
        )
        assertEquals(200, shopping.first)
        val work = exchange(
            "POST",
            "/api/reminders",
            """{"message":"Enviar relatório","listId":3,"date":"","time":""}""",
            token,
        )
        assertEquals(200, work.first)

        val all = exchange("GET", "/api/reminders", token = token).second
        assertTrue(all.contains("Todas") || all.contains("\"name\":\"Lembretes\""))
        assertTrue(all.contains("\"name\":\"Compras\""))
        assertTrue(all.contains("\"name\":\"Trabalho\""))
        assertTrue(all.contains("Comprar café"))
        assertTrue(all.contains("Enviar relatório"))
        assertTrue(all.contains("Sem prazo"))
        assertTrue(all.contains("\"hasDeadline\":false"))
        assertTrue(all.contains("\"completed\":true"))

        val onlyShopping = exchange("GET", "/api/reminders?listId=2", token = token).second
        assertTrue(onlyShopping.contains("Comprar café"))
        assertFalse(onlyShopping.contains("Enviar relatório"))
        assertFalse(onlyShopping.contains("Levar documentos"))

        val moved = exchange(
            "PUT",
            "/api/reminders/1",
            """{"message":"Comprar café especial","listId":3,"date":"","time":""}""",
            token,
        )
        assertEquals(200, moved.first)
        val onlyWork = exchange("GET", "/api/reminders?listId=3", token = token).second
        assertTrue(onlyWork.contains("Comprar café especial"))
        assertTrue(onlyWork.contains("Enviar relatório"))
        val shoppingAfter = exchange("GET", "/api/reminders?listId=2", token = token).second
        assertFalse(shoppingAfter.contains("Comprar café"))
    }

    @Test
    fun keepsAccentedCharactersAndListColors() {
        val token = unlock()
        val created = exchange(
            method = "POST",
            path = "/api/reminders",
            body = """{"message":"Passar cartão para anúncios","listId":1,"date":"","time":""}""",
            token = token,
            contentType = "application/json",
        )
        assertEquals(created.second, 200, created.first)
        val listed = exchange("GET", "/api/reminders", token = token).second
        assertTrue(listed.contains("Passar cartão para anúncios"))
        assertFalse(listed.contains("\uFFFD"))
        assertTrue(listed.contains("\"color\":\"#6C3CE9\""))
        assertTrue(listed.contains("\"color\":\"#1F8A5B\""))
        assertTrue(listed.contains("\"color\":\"#2F6FBE\""))
        assertTrue(listed.contains("\"color\":\"#C47A22\""))
    }

    private fun unlock(): String {
        val (code, body) = exchange("POST", "/api/session", """{"pin":"4821"}""")
        assertEquals(200, code)
        return tokenFrom(body) ?: error("token ausente")
    }

    private fun tokenFrom(body: String): String? {
        return Regex(""""token":"([0-9a-f]+)"""").find(body)?.groupValues?.get(1)
    }

    private fun startServer(): LocalAccessServer {
        var last: Exception? = null
        repeat(5) {
            val candidate = ServerSocket(0).use { it.localPort }
            val created = LocalAccessServer(
                hostname = "127.0.0.1",
                port = candidate,
                pin = "4821",
                actions = memory,
                zone = zone,
                now = { nowMillis },
            )
            try {
                created.start()
                port = candidate
                return created
            } catch (error: Exception) {
                last = error
                created.stop()
            }
        }
        throw last ?: IllegalStateException("Não foi possível abrir a porta de teste.")
    }

    private fun exchange(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        contentType: String = "application/json; charset=utf-8",
    ): Pair<Int, String> {
        val connection = (URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 4000
            readTimeout = 4000
            instanceFollowRedirects = false
            if (token != null) setRequestProperty("X-Local-Token", token)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        if (body != null) {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        val stream = connection.errorStream ?: connection.inputStream
        val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        connection.disconnect()
        return code to text
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private class MemoryReminders : LocalReminderActions {
        val items = mutableListOf<Reminder>()
        val lists = mutableListOf(
            TaskList(id = 1, name = "Lembretes", isDefault = true, createdAt = 1),
            TaskList(id = 2, name = "Compras", isDefault = true, createdAt = 2),
            TaskList(id = 3, name = "Trabalho", isDefault = true, createdAt = 3),
            TaskList(id = 4, name = "Ideias", isDefault = true, createdAt = 4),
        )
        private var nextId = 1L

        override suspend fun lists(): List<TaskList> = lists.toList()

        override suspend fun list(): List<Reminder> = items.toList()

        override suspend fun add(message: String, scheduledAt: Long?, listId: Long): Result<Long> {
            val id = nextId++
            items += Reminder(
                id = id,
                message = message.trim(),
                scheduledAt = scheduledAt,
                listId = listId,
            )
            return Result.success(id)
        }

        override suspend fun update(
            id: Long,
            message: String,
            scheduledAt: Long?,
            listId: Long,
        ): Result<Unit> {
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) return Result.failure(LocalAccessFailure.Missing())
            val current = items[index]
            items[index] = current.copy(
                message = message.trim(),
                scheduledAt = scheduledAt,
                triggered = false,
                listId = listId,
            )
            return Result.success(Unit)
        }

        override suspend fun snoozeOneHour(id: Long): Result<Unit> {
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) return Result.failure(LocalAccessFailure.Missing())
            val current = items[index]
            val currentTime = current.scheduledAt ?: return Result.failure(LocalAccessFailure.NoDeadline())
            val newTime = maxOf(System.currentTimeMillis(), currentTime) + 60 * 60 * 1000L
            items[index] = current.copy(scheduledAt = newTime, triggered = false)
            return Result.success(Unit)
        }

        override suspend fun delete(id: Long): Result<Unit> {
            val removed = items.removeAll { it.id == id }
            return if (removed) Result.success(Unit) else Result.failure(LocalAccessFailure.Missing())
        }
    }
}
