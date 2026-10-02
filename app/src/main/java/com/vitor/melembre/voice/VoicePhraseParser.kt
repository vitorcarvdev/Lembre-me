package com.vitor.melembre.voice

import com.vitor.melembre.data.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object VoicePhraseParser {
    private val locale = Locale.forLanguageTag("pt-BR")

    // \b do Java não trata bem acentos (ã, ç…). Use fronteira Unicode de letra.
    private fun word(pattern: String): Regex =
        Regex("(?<!\\p{L})(?:$pattern)(?!\\p{L})", RegexOption.IGNORE_CASE)

    fun parse(
        spokenText: String,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): ParsedVoiceReminder? {
        val normalized = normalize(spokenText)
        if (normalized.isBlank()) return null

        val recurrence = detectRecurrence(normalized)
        val time = extractTime(normalized) ?: return null
        val date = extractDate(normalized, today) ?: defaultDateForTime(today, time)
        val message = extractMessage(normalized) ?: return null

        return ParsedVoiceReminder(
            message = message.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
            date = date,
            time = time,
            recurrence = recurrence,
            rawText = spokenText.trim(),
        )
    }

    private fun normalize(text: String): String {
        return text
            .lowercase(locale)
            .replace(Regex("[\\n\\r]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectRecurrence(text: String): Recurrence {
        return when {
            word("todo dia|diariamente|todos os dias").containsMatchIn(text) -> Recurrence.DAILY
            word("toda semana|semanalmente|todas as semanas").containsMatchIn(text) -> Recurrence.WEEKLY
            word("todo m[eê]s|mensalmente|todos os meses").containsMatchIn(text) -> Recurrence.MONTHLY
            word("todo ano|anualmente|todos os anos").containsMatchIn(text) -> Recurrence.YEARLY
            else -> Recurrence.NONE
        }
    }

    private fun extractTime(text: String): LocalTime? {
        Regex("(?<!\\p{L})(\\d{1,2})[:hH](\\d{2})(?!\\p{L})").find(text)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) return LocalTime.of(hour, minute)
        }

        Regex(
            "(?<!\\p{L})(?:às|as)\\s+(\\d{1,2})\\s*(?:horas?|hrs?)?\\s*(da manhã|da manha|da tarde|da noite|da madrugada)?(?!\\p{L})",
        ).find(text)?.let { match ->
            return interpretSpokenHour(
                hour = match.groupValues[1].toInt(),
                period = match.groupValues[2].ifBlank { null },
            )
        }

        Regex(
            "(?<!\\p{L})(\\d{1,2})\\s*(?:horas?|hrs?)\\s*(da manhã|da manha|da tarde|da noite|da madrugada)?(?!\\p{L})",
        ).find(text)?.let { match ->
            return interpretSpokenHour(
                hour = match.groupValues[1].toInt(),
                period = match.groupValues[2].ifBlank { null },
            )
        }

        return null
    }

    private fun interpretSpokenHour(hour: Int, period: String?): LocalTime? {
        if (hour !in 0..24) return null
        var h = hour % 24
        when (period) {
            "da manhã", "da manha", "da madrugada" -> if (h == 12) h = 0
            "da tarde", "da noite" -> if (h in 1..11) h += 12
        }
        if (h !in 0..23) return null
        return LocalTime.of(h, 0)
    }

    private fun extractDate(text: String, today: LocalDate): LocalDate? {
        if (word("hoje").containsMatchIn(text)) return today
        if (word("amanh[ãa]").containsMatchIn(text)) return today.plusDays(1)

        Regex("(?<!\\p{L})(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?(?!\\p{L})").find(text)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val yearRaw = match.groupValues[3]
            val year = when {
                yearRaw.isBlank() -> {
                    val y = today.year
                    val candidate = runCatching { LocalDate.of(y, month, day) }.getOrNull()
                    if (candidate != null && candidate.isBefore(today)) y + 1 else y
                }
                yearRaw.length == 2 -> 2000 + yearRaw.toInt()
                else -> yearRaw.toInt()
            }
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        val weekday = detectWeekday(text) ?: return null
        var date = today.with(TemporalAdjusters.nextOrSame(weekday))
        if (date.isBefore(today) || (date == today && word("pr[oó]xim[oa]").containsMatchIn(text))) {
            date = today.with(TemporalAdjusters.next(weekday))
        }
        return date
    }

    private fun detectWeekday(text: String): DayOfWeek? {
        return when {
            word("segunda(-feira)?").containsMatchIn(text) -> DayOfWeek.MONDAY
            word("ter[cç]a(-feira)?").containsMatchIn(text) -> DayOfWeek.TUESDAY
            word("quarta(-feira)?").containsMatchIn(text) -> DayOfWeek.WEDNESDAY
            word("quinta(-feira)?").containsMatchIn(text) -> DayOfWeek.THURSDAY
            word("sexta(-feira)?").containsMatchIn(text) -> DayOfWeek.FRIDAY
            word("s[áa]bado").containsMatchIn(text) -> DayOfWeek.SATURDAY
            word("domingo").containsMatchIn(text) -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun defaultDateForTime(today: LocalDate, time: LocalTime): LocalDate {
        val now = java.time.LocalDateTime.now()
        return if (today.atTime(time).isAfter(now)) today else today.plusDays(1)
    }

    private fun extractMessage(text: String): String? {
        var working = text
            .replace(Regex("^(me\\s+lembre|lembr[ea]\\s*-?me|criar\\s+lembrete)\\s*"), "")
            .trim()

        val junkPatterns = listOf(
            "todo dia|diariamente|todos os dias|toda semana|semanalmente|todas as semanas|todo m[eê]s|mensalmente|todos os meses|todo ano|anualmente|todos os anos",
            "hoje|amanh[ãa]|segunda(-feira)?|ter[cç]a(-feira)?|quarta(-feira)?|quinta(-feira)?|sexta(-feira)?|s[áa]bado|domingo",
            "\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?",
            "(?:às|as)\\s+\\d{1,2}(?:[:hH]\\d{2})?\\s*(?:horas?|hrs?)?\\s*(da manhã|da manha|da tarde|da noite|da madrugada)?",
            "\\d{1,2}[:hH]\\d{2}",
            "\\d{1,2}\\s*(?:horas?|hrs?)\\s*(da manhã|da manha|da tarde|da noite|da madrugada)?",
            "da manhã|da manha|da tarde|da noite|da madrugada",
        )

        junkPatterns.forEach { pattern ->
            working = working.replace(word(pattern), " ")
        }

        working = working
            .replace(Regex("^\\s*(de|do|da|para|pra)\\s+"), "")
            .replace(Regex("\\s+(de|do|da|para|pra)\\s*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', ':')

        return working.ifBlank { null }
    }
}
