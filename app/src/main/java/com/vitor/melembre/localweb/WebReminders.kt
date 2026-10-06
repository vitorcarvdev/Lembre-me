package com.vitor.melembre.localweb

import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.ui.combineDateAndTime
import com.vitor.melembre.util.ListColors
import com.vitor.melembre.util.ReminderOrdering
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object WebReminders {
    private val locale = Locale.forLanguageTag("pt-BR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

    fun remindersPayload(
        reminders: List<Reminder>,
        lists: List<TaskList>,
        nowMillis: Long,
        zone: ZoneId,
        listFilter: Long? = null,
    ): String {
        val visible = if (listFilter == null) {
            reminders
        } else {
            reminders.filter { it.listId == listFilter }
        }
        val body = ReminderOrdering.ordered(visible, nowMillis).joinToString(",") { reminder ->
            reminderJson(reminder, lists, nowMillis, zone)
        }
        val listBody = lists.joinToString(",") { list ->
            "{\"id\":${list.id},\"name\":${quoted(list.name)},\"isDefault\":${list.isDefault},\"color\":${quoted(ListColors.hex(list.name))}}"
        }
        return """{"lists":[$listBody],"reminders":[$body]}"""
    }

    private fun reminderJson(
        reminder: Reminder,
        lists: List<TaskList>,
        nowMillis: Long,
        zone: ZoneId,
    ): String {
        val scheduledAt = reminder.scheduledAt
        val completed = ReminderOrdering.isCompleted(reminder, nowMillis)
        val listName = lists.firstOrNull { it.id == reminder.listId }?.name.orEmpty()
        val date: String
        val time: String
        val schedule: String
        if (scheduledAt == null) {
            date = ""
            time = ""
            schedule = "Sem prazo"
        } else {
            val dateTime = Instant.ofEpochMilli(scheduledAt).atZone(zone)
            date = dateTime.toLocalDate().toString()
            time = timeFormatter.format(dateTime.toLocalTime().withSecond(0).withNano(0))
            schedule = formatWebSchedule(scheduledAt, nowMillis, zone, reminder.recurrenceType.shortLabelPt)
        }
        return buildString {
            append("{\"id\":")
            append(reminder.id)
            append(",\"message\":")
            append(quoted(reminder.message))
            append(",\"listId\":")
            append(reminder.listId)
            append(",\"listName\":")
            append(quoted(listName))
            append(",\"date\":")
            append(quoted(date))
            append(",\"time\":")
            append(quoted(time))
            append(",\"schedule\":")
            append(quoted(schedule))
            append(",\"hasDeadline\":")
            append(if (scheduledAt != null) "true" else "false")
            append(",\"completed\":")
            append(if (completed) "true" else "false")
            append('}')
        }
    }

    fun formatWebSchedule(
        scheduledAtMillis: Long,
        nowMillis: Long,
        zone: ZoneId,
        recurrenceLabel: String? = null,
    ): String {
        val dateTime = Instant.ofEpochMilli(scheduledAtMillis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val day = when (val date = dateTime.toLocalDate()) {
            today -> "Hoje"
            today.plusDays(1) -> "Amanhã"
            today.minusDays(1) -> "Ontem"
            else -> dateFormatter.format(date)
        }
        val base = "$day ${timeFormatter.format(dateTime)}"
        return if (recurrenceLabel.isNullOrBlank()) base else "$base · $recurrenceLabel"
    }

    fun parseDateTime(date: String, time: String, zone: ZoneId): Long? {
        val localDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull() ?: return null
        val localTime = runCatching { LocalTime.parse(time.trim()) }.getOrNull() ?: return null
        return combineDateAndTime(localDate, localTime.withSecond(0).withNano(0), zone)
    }

    private fun quoted(value: String): String = "\"${MiniJson.escape(value)}\""
}
