package com.vitor.melembre.localweb

import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.util.ReminderOrdering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class LocalAccessLogicTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun rejectsLoopbackAndLinkLocalAddresses() {
        assertFalse(LocalIp.isUsableLanIpv4("127.0.0.1"))
        assertFalse(LocalIp.isUsableLanIpv4("169.254.1.10"))
        assertFalse(LocalIp.isUsableLanIpv4("0.0.0.0"))
        assertFalse(LocalIp.isUsableLanIpv4("::1"))
        assertTrue(LocalIp.isUsableLanIpv4("192.168.0.25"))
        assertNull(LocalIp.pickLanIpv4(listOf("127.0.0.1", null, "169.254.1.1")))
        assertEquals(
            "192.168.0.25",
            LocalIp.pickLanIpv4(listOf("127.0.0.1", "192.168.0.25")),
        )
    }

    @Test
    fun fixedPinKeepsLeadingZerosAndSurvivesReload() {
        assertEquals("0001", LocalPin.DEFAULT)
        assertEquals("0001", LocalPin.normalize(null))
        assertEquals("0001", LocalPin.normalize("0001"))
        assertEquals("0001", LocalPin.normalize("1"))
        assertFalse(LocalPin.isValid("1"))
        assertFalse(LocalPin.isValid("00001"))
        assertFalse(LocalPin.isValid("00a1"))
        assertTrue(LocalPin.isValid("0001"))

        val prefs = MemoryPinPreferences()
        assertEquals("0001", LocalPin.read(prefs))
        assertTrue(LocalPin.save(prefs, "0001"))
        assertEquals("0001", LocalPin.read(prefs))

        assertTrue(LocalPin.save(prefs, "0042"))
        assertEquals("0042", LocalPin.read(prefs))
        assertFalse(LocalPin.save(prefs, "42"))
        assertEquals("0042", LocalPin.read(prefs))

        val reloaded = LocalPin.read(prefs)
        assertEquals("0042", reloaded)

        val gate = LocalAccessGate("0001")
        assertNull(gate.unlock("1"))
        assertTrue(gate.unlock("0001") != null)
    }

    @Test
    fun gateKeepsSessionInMemory() {
        val gate = LocalAccessGate("4821")
        assertNull(gate.unlock("0000"))
        assertFalse(gate.allows(null))
        val token = gate.unlock("4821")
        assertTrue(token != null && token.length == 32)
        assertTrue(gate.allows(token))
        assertFalse(gate.allows("outro"))
    }

    @Test
    fun parsesFlatJson() {
        val parsed = MiniJson.parseFlatObject(
            """{"message":"comprar \"pao\"","date":"2026-10-05","time":"09:00","pin":4821}""",
        )
        assertEquals("comprar \"pao\"", parsed?.get("message"))
        assertEquals("2026-10-05", parsed?.get("date"))
        assertEquals("09:00", parsed?.get("time"))
        assertEquals("4821", parsed?.get("pin"))
        assertEquals("pão", MiniJson.parseFlatObject("""{"message":"p\u00e3o"}""")?.get("message"))
    }

    @Test
    fun formatsScheduleLikeTheWebList() {
        val now = at(2026, 10, 5, 8, 0)
        assertEquals("Hoje 09:00", WebReminders.formatWebSchedule(at(2026, 10, 5, 9, 0), now, zone))
        assertEquals("Ontem 10:00", WebReminders.formatWebSchedule(at(2026, 10, 4, 10, 0), now, zone))
        assertEquals("Amanhã 14:00", WebReminders.formatWebSchedule(at(2026, 10, 6, 14, 0), now, zone))
        assertEquals("01/10/2026 08:30", WebReminders.formatWebSchedule(at(2026, 10, 1, 8, 30), now, zone))
        assertEquals(
            "Hoje 09:00 · Diário",
            WebReminders.formatWebSchedule(at(2026, 10, 5, 9, 0), now, zone, "Diário"),
        )
    }

    @Test
    fun ordersActiveBeforeCompleted() {
        val now = at(2026, 10, 5, 12, 0)
        val past = Reminder(id = 1, message = "Levar documentos", scheduledAt = at(2026, 10, 4, 10, 0), triggered = true)
        val later = Reminder(id = 2, message = "Pagar internet", scheduledAt = at(2026, 10, 5, 18, 0))
        val sooner = Reminder(id = 3, message = "Comprar pão", scheduledAt = at(2026, 10, 5, 15, 0))
        val ordered = ReminderOrdering.ordered(listOf(past, later, sooner), now)
        assertEquals(listOf(3L, 2L, 1L), ordered.map { it.id })
        assertFalse(ReminderOrdering.isCompleted(sooner, now))
        assertTrue(ReminderOrdering.isCompleted(past, now))
        val lists = listOf(TaskList(id = 1, name = "Lembretes", isDefault = true, createdAt = 1))
        val json = WebReminders.remindersPayload(listOf(past, sooner), lists, now, zone)
        assertTrue(json.indexOf("Comprar pão") < json.indexOf("Levar documentos"))
        assertTrue(json.contains("\"completed\":false"))
        assertTrue(json.contains("\"completed\":true"))
        assertTrue(json.contains("Ontem 10:00"))
        assertTrue(json.contains("\"name\":\"Lembretes\""))
        assertTrue(json.contains("\"color\":\"#6C3CE9\""))
        val undated = Reminder(id = 4, message = "Quando puder", scheduledAt = null, listId = 1)
        val undatedJson = WebReminders.remindersPayload(listOf(undated), lists, now, zone)
        assertTrue(undatedJson.contains("Sem prazo"))
        assertTrue(undatedJson.contains("\"hasDeadline\":false"))
        assertFalse(ReminderOrdering.isCompleted(undated, now))
    }

    private class MemoryPinPreferences : PinPreferences {
        private val values = HashMap<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(key: String, value: String) {
            values[key] = value
        }
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
