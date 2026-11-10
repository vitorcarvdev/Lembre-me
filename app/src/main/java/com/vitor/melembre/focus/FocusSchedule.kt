package com.vitor.melembre.focus

data class FocusBlock(
    val name: String,
    val startMin: Int,
    val endMin: Int,
    /** Bloco inteiro de descanso (ex.: almoço), sem ciclos de foco. */
    val rest: Boolean = false,
)

enum class FocusPhase { FOCUS, BREAK, REST, IDLE }

data class FocusMoment(
    val phase: FocusPhase,
    val block: FocusBlock?,
    val blockIndex: Int,
    val remainingSec: Int,
    val cycle: Int,
    val cycles: Int,
    val blockFraction: Float,
) {
    /** Muda sempre que a fase muda (usado para tocar o som e trocar o fundo). */
    val key: String get() = "$blockIndex:$phase:$cycle"
}

object FocusSchedule {
    const val FOCUS_MIN = 25
    const val BREAK_MIN = 5
    private const val MAX_BLOCKS = 24
    private const val MAX_NAME = 40

    val DEFAULT: List<FocusBlock> = listOf(
        FocusBlock("Produção VitAgencia", 8 * 60, 10 * 60),
        FocusBlock("VitProspect 1", 10 * 60, 11 * 60),
        FocusBlock("VitProspect 2", 11 * 60, 12 * 60),
        FocusBlock("Almoço", 12 * 60, 13 * 60, rest = true),
        FocusBlock("Vendas Clientes VCSis", 13 * 60, 15 * 60),
        FocusBlock("VitProspect 3", 15 * 60, 17 * 60),
    )

    fun momentAt(blocks: List<FocusBlock>, secondOfDay: Int): FocusMoment {
        val index = blocks.indexOfFirst { secondOfDay >= it.startMin * 60 && secondOfDay < it.endMin * 60 }
        if (index < 0) return FocusMoment(FocusPhase.IDLE, null, -1, 0, 0, 0, 0f)
        val block = blocks[index]
        val start = block.startMin * 60
        val end = block.endMin * 60
        val fraction = (secondOfDay - start).toFloat() / (end - start)
        if (block.rest) {
            return FocusMoment(FocusPhase.REST, block, index, end - secondOfDay, 0, 0, fraction)
        }
        val focusLen = FOCUS_MIN * 60
        val cycleLen = (FOCUS_MIN + BREAK_MIN) * 60
        val elapsed = secondOfDay - start
        val within = elapsed % cycleLen
        val cycles = (end - start + cycleLen - 1) / cycleLen
        val cycle = elapsed / cycleLen + 1
        val phase = if (within < focusLen) FocusPhase.FOCUS else FocusPhase.BREAK
        val phaseEnd = if (phase == FocusPhase.FOCUS) focusLen else cycleLen
        val remaining = minOf(phaseEnd - within, end - secondOfDay)
        return FocusMoment(phase, block, index, remaining, cycle, cycles, fraction)
    }

    /** Próximo bloco que ainda vai começar (o de início mais cedo depois de agora), ou null. */
    fun nextBlock(blocks: List<FocusBlock>, secondOfDay: Int): FocusBlock? =
        blocks.filter { it.startMin * 60 > secondOfDay }.minByOrNull { it.startMin }

    fun formatMinutes(minutes: Int): String ="%02d:%02d".format(minutes / 60, minutes % 60)

    fun parseMinutes(text: String): Int? {
        val match = Regex("^(\\d{1,2}):(\\d{2})$").find(text.trim()) ?: return null
        val h = match.groupValues[1].toInt()
        val m = match.groupValues[2].toInt()
        if (m > 59 || h > 24 || (h == 24 && m != 0)) return null
        return h * 60 + m
    }

    fun cleanName(raw: String): String =
        raw.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim().take(MAX_NAME)

    /** Linhas "HH:MM|HH:MM|0 ou 1 (descanso)|Nome". */
    fun encode(blocks: List<FocusBlock>): String = blocks.joinToString("\n") {
        "${formatMinutes(it.startMin)}|${formatMinutes(it.endMin)}|${if (it.rest) 1 else 0}|${cleanName(it.name)}"
    }

    fun decode(text: String): List<FocusBlock>? {
        val blocks = mutableListOf<FocusBlock>()
        for (line in text.lines()) {
            if (line.isBlank()) continue
            val parts = line.split('|', limit = 4)
            if (parts.size != 4) return null
            val start = parseMinutes(parts[0]) ?: return null
            val end = parseMinutes(parts[1]) ?: return null
            val rest = when (parts[2].trim()) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            blocks += FocusBlock(cleanName(parts[3]), start, end, rest)
        }
        return blocks.sortedBy { it.startMin }
    }

    /** Retorna a mensagem de erro, ou null se a agenda é válida. */
    fun validate(blocks: List<FocusBlock>): String? {
        if (blocks.size > MAX_BLOCKS) return "Use no máximo $MAX_BLOCKS blocos."
        val sorted = blocks.sortedBy { it.startMin }
        sorted.forEachIndexed { i, b ->
            if (b.name.isBlank()) return "Dê um nome a cada bloco."
            if (b.endMin <= b.startMin) return "O fim de \"${b.name}\" precisa ser depois do início."
            if (i > 0 && b.startMin < sorted[i - 1].endMin) {
                return "\"${sorted[i - 1].name}\" e \"${b.name}\" se sobrepõem."
            }
        }
        return null
    }
}
