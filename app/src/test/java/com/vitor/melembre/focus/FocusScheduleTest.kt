package com.vitor.melembre.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FocusScheduleTest {
    private val agenda = FocusSchedule.DEFAULT

    private fun at(h: Int, m: Int, s: Int = 0) = FocusSchedule.momentAt(agenda, h * 3600 + m * 60 + s)

    @Test
    fun focusAndBreakCyclesInsideBlock() {
        val start = at(10, 0)
        assertEquals(FocusPhase.FOCUS, start.phase)
        assertEquals("VitProspect 1", start.block?.name)
        assertEquals(25 * 60, start.remainingSec)
        assertEquals(1, start.cycle)
        assertEquals(2, start.cycles)

        val focusEnd = at(10, 24, 59)
        assertEquals(FocusPhase.FOCUS, focusEnd.phase)
        assertEquals(1, focusEnd.remainingSec)

        val breakStart = at(10, 25)
        assertEquals(FocusPhase.BREAK, breakStart.phase)
        assertEquals(5 * 60, breakStart.remainingSec)

        val second = at(10, 30)
        assertEquals(FocusPhase.FOCUS, second.phase)
        assertEquals(2, second.cycle)

        val lastBreak = at(10, 56)
        assertEquals(FocusPhase.BREAK, lastBreak.phase)
        assertEquals(4 * 60, lastBreak.remainingSec)
    }

    @Test
    fun blockBoundaryChangesBlock() {
        assertEquals("VitProspect 1", at(10, 59, 59).block?.name)
        val next = at(11, 0)
        assertEquals("VitProspect 2", next.block?.name)
        assertEquals(FocusPhase.FOCUS, next.phase)
        assertEquals(1, next.cycle)
    }

    @Test
    fun lunchIsOneLongRestWithoutCycles() {
        val lunch = at(12, 19)
        assertEquals(FocusPhase.REST, lunch.phase)
        assertEquals("Almoço", lunch.block?.name)
        assertEquals(41 * 60, lunch.remainingSec)
        assertEquals(0, lunch.cycles)
    }

    @Test
    fun nextBlockIsTheOneStartingAfterNow() {
        val a = FocusSchedule.DEFAULT
        assertEquals("VitProspect 1", FocusSchedule.nextBlock(a, 9 * 3600)?.name)
        assertEquals("Vendas Clientes VCSis", FocusSchedule.nextBlock(a, 12 * 3600 + 600)?.name)
        assertEquals("Produção VitAgencia", FocusSchedule.nextBlock(a, 6 * 3600)?.name)
        assertNull(FocusSchedule.nextBlock(a, 16 * 3600))
    }

    @Test
    fun outsideAgendaIsIdle() {
        assertEquals(FocusPhase.IDLE, at(7, 59, 59).phase)
        assertEquals(FocusPhase.IDLE, at(17, 0).phase)
        assertNull(at(18, 7).block)
    }

    @Test
    fun phaseKeyChangesOnlyWhenPhaseChanges() {
        assertEquals(at(10, 5).key, at(10, 20).key)
        assert(at(10, 24, 59).key != at(10, 25).key)
        assert(at(10, 29, 59).key != at(10, 30).key)
    }

    @Test
    fun lastPartialPhaseIsCutAtBlockEnd() {
        val custom = listOf(FocusBlock("Curto", 9 * 60, 9 * 60 + 40))
        val m = FocusSchedule.momentAt(custom, 9 * 3600 + 36 * 60)
        assertEquals(FocusPhase.FOCUS, m.phase)
        assertEquals(4 * 60, m.remainingSec)
        assertEquals(2, m.cycles)
    }

    @Test
    fun encodeDecodeRoundTripAndSorting() {
        val text = FocusSchedule.encode(FocusSchedule.DEFAULT)
        assertEquals(FocusSchedule.DEFAULT, FocusSchedule.decode(text))
        val shuffled = "13:00|15:00|0|Tarde\n08:00|12:00|0|Manhã"
        assertEquals(listOf("Manhã", "Tarde"), FocusSchedule.decode(shuffled)?.map { it.name })
    }

    @Test
    fun decodeRejectsBrokenLines() {
        assertNull(FocusSchedule.decode("08:00|10:00|Nome"))
        assertNull(FocusSchedule.decode("25:00|26:00|0|X"))
        assertNull(FocusSchedule.decode("08:00|10:00|2|X"))
        assertNotNull(FocusSchedule.decode("\n  \n"))
    }

    @Test
    fun validateCatchesOverlapAndOrder() {
        assertNull(FocusSchedule.validate(FocusSchedule.DEFAULT))
        val overlap = listOf(FocusBlock("A", 480, 600), FocusBlock("B", 590, 700))
        assertNotNull(FocusSchedule.validate(overlap))
        assertNotNull(FocusSchedule.validate(listOf(FocusBlock("A", 600, 600))))
        assertNotNull(FocusSchedule.validate(listOf(FocusBlock(" ", 480, 600))))
    }

    @Test
    fun namesAreCleaned() {
        assertEquals("A B", FocusSchedule.cleanName("A|B\n"))
        assertEquals(40, FocusSchedule.cleanName("x".repeat(80)).length)
    }
}
