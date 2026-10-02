package com.vitor.melembre.voice

import com.vitor.melembre.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VoicePhraseParserTest {
    private val today = LocalDate.of(2026, 10, 2)

    @Test
    fun parsesTomorrowAfternoonReminder() {
        val parsed = VoicePhraseParser.parse(
            spokenText = "Me lembre amanhã de colocar o lixo para fora às 17 horas",
            today = today,
        )
        assertNotNull(parsed)
        assertEquals("Colocar o lixo para fora", parsed!!.message)
        assertEquals(LocalDate.of(2026, 10, 3), parsed.date)
        assertEquals(LocalTime.of(17, 0), parsed.time)
        assertEquals(Recurrence.NONE, parsed.recurrence)
    }

    @Test
    fun parsesMorningReminder() {
        val parsed = VoicePhraseParser.parse(
            spokenText = "Me lembre amanhã de pagar a conta às 10 da manhã",
            today = today,
        )
        assertNotNull(parsed)
        assertEquals("Pagar a conta", parsed!!.message)
        assertEquals(LocalDate.of(2026, 10, 3), parsed.date)
        assertEquals(LocalTime.of(10, 0), parsed.time)
    }

    @Test
    fun parsesDailyRecurrence() {
        val parsed = VoicePhraseParser.parse(
            spokenText = "Me lembre todo dia de tomar o remédio às 08:30",
            today = today,
        )
        assertNotNull(parsed)
        assertEquals("Tomar o remédio", parsed!!.message)
        assertEquals(LocalTime.of(8, 30), parsed.time)
        assertEquals(Recurrence.DAILY, parsed.recurrence)
    }

    @Test
    fun rejectsPhraseWithoutTime() {
        assertNull(
            VoicePhraseParser.parse(
                spokenText = "Me lembre amanhã de comprar pão",
                today = today,
            ),
        )
    }
}
