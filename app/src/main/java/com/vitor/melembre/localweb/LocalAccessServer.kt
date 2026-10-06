package com.vitor.melembre.localweb

import com.vitor.melembre.util.ReminderValidation
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.ZoneId

class LocalAccessServer(
    hostname: String,
    port: Int,
    pin: String,
    private val actions: LocalReminderActions,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = { System.currentTimeMillis() },
    private val pageHtml: String = LocalAccessPage.HTML,
) : NanoHTTPD(hostname, port) {
    private val gate = LocalAccessGate(pin)
    private val reminderPath = Regex("^/api/reminders/(\\d+)$")
    private val snoozePath = Regex("^/api/reminders/(\\d+)/snooze$")

    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        val path = session.uri.substringBefore('?')
        return try {
            when {
                method == Method.GET && path == "/" -> page()
                method == Method.POST && path == "/api/session" -> unlock(session)
                method == Method.GET && path == "/api/reminders" -> guarded(session) { listResponse(session) }
                method == Method.POST && path == "/api/reminders" -> guarded(session) { create(session) }
                method == Method.PUT && reminderPath.matches(path) -> {
                    val id = reminderPath.find(path)!!.groupValues[1].toLong()
                    guarded(session) { update(session, id) }
                }
                method == Method.POST && snoozePath.matches(path) -> {
                    val id = snoozePath.find(path)!!.groupValues[1].toLong()
                    guarded(session) { mutate { actions.snoozeOneHour(id) } }
                }
                method == Method.DELETE && reminderPath.matches(path) -> {
                    val id = reminderPath.find(path)!!.groupValues[1].toLong()
                    guarded(session) { mutate { actions.delete(id) } }
                }
                else -> json(Response.Status.NOT_FOUND, errorBody("Não encontrado."))
            }
        } catch (_: Exception) {
            json(Response.Status.INTERNAL_ERROR, errorBody("Falha interna."))
        }
    }

    private fun page(): Response {
        val response = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", pageHtml)
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun unlock(session: IHTTPSession): Response {
        val fields = readFields(session) ?: return unauthorized()
        val token = gate.unlock(fields["pin"].orEmpty()) ?: return unauthorized()
        return json(Response.Status.OK, """{"ok":true,"token":"$token"}""")
    }

    private fun listResponse(session: IHTTPSession): Response {
        val nowMillis = now()
        val filter = session.parameters["listId"]?.firstOrNull()?.toLongOrNull()
        val reminders = blocking { actions.list() }
        val lists = blocking { actions.lists() }
        return json(
            Response.Status.OK,
            WebReminders.remindersPayload(reminders, lists, nowMillis, zone, filter),
        )
    }

    private fun create(session: IHTTPSession): Response {
        val fields = readFields(session) ?: return badRequest("Não foi possível salvar.")
        return save(null, fields)
    }

    private fun update(session: IHTTPSession, id: Long): Response {
        val fields = readFields(session) ?: return badRequest("Não foi possível salvar.")
        return save(id, fields)
    }

    private fun save(existingId: Long?, fields: Map<String, String>): Response {
        val message = fields["message"].orEmpty()
        if (!ReminderValidation.isMessageValid(message)) {
            return badRequest("Digite uma mensagem.")
        }
        val listId = fields["listId"]?.toLongOrNull() ?: return badRequest("Escolha uma lista.")
        val date = fields["date"].orEmpty().trim()
        val time = fields["time"].orEmpty().trim()
        val scheduledAt = when {
            date.isEmpty() && time.isEmpty() -> null
            date.isEmpty() || time.isEmpty() -> return badRequest("Data ou hora inválida.")
            else -> WebReminders.parseDateTime(date, time, zone)
                ?: return badRequest("Data ou hora inválida.")
        }
        if (scheduledAt != null && !ReminderValidation.isScheduledInFuture(scheduledAt, now())) {
            return badRequest("Escolha uma data e horário futuros.")
        }
        return mutate {
            if (existingId == null) {
                actions.add(message, scheduledAt, listId)
            } else {
                actions.update(existingId, message, scheduledAt, listId)
            }
        }
    }

    private fun mutate(block: suspend () -> Result<*>): Response {
        val result = blocking(block)
        return result.fold(
            onSuccess = { json(Response.Status.OK, """{"ok":true}""") },
            onFailure = { error ->
                when (error) {
                    is LocalAccessFailure.Missing -> json(
                        Response.Status.NOT_FOUND,
                        errorBody("Lembrete não encontrado."),
                    )
                    is LocalAccessFailure.ExactAlarm -> json(
                        Response.Status.CONFLICT,
                        errorBody("Ative alarmes exatos nas configurações do Android."),
                    )
                    is LocalAccessFailure.NoDeadline -> badRequest("Esta tarefa não tem prazo.")
                    else -> badRequest("Não foi possível salvar.")
                }
            },
        )
    }

    private fun guarded(session: IHTTPSession, block: () -> Response): Response {
        if (!gate.allows(session.headers["x-local-token"])) return unauthorized()
        return block()
    }

    private fun readFields(session: IHTTPSession): Map<String, String>? {
        return try {
            val length = session.headers["content-length"]?.toIntOrNull() ?: return null
            if (length <= 0 || length > MAX_BODY_BYTES) return null
            val buffer = ByteArray(length)
            val input = session.inputStream
            var offset = 0
            while (offset < length) {
                val read = input.read(buffer, offset, length - offset)
                if (read < 0) break
                offset += read
            }
            MiniJson.parseFlatObject(String(buffer, 0, offset, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    private fun unauthorized(): Response {
        return json(Response.Status.UNAUTHORIZED, errorBody("PIN incorreto."))
    }

    private fun badRequest(message: String): Response {
        return json(Response.Status.BAD_REQUEST, errorBody(message))
    }

    private fun errorBody(message: String): String {
        return """{"ok":false,"error":"${MiniJson.escape(message)}"}"""
    }

    private fun json(status: Response.Status, body: String): Response {
        val response = newFixedLengthResponse(status, "application/json; charset=utf-8", body)
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun <T> blocking(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private companion object {
        const val MAX_BODY_BYTES = 1_000_000
    }
}
