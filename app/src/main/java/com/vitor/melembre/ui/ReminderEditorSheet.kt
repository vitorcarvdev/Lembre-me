package com.vitor.melembre.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.vitor.melembre.util.ReminderUrl
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.util.ListColors
import com.vitor.melembre.util.ReminderOrdering
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))

private fun listColor(lists: List<TaskList>, id: Long?): Color {
    val name = lists.firstOrNull { it.id == id }?.name.orEmpty()
    return Color(0xFF000000L or ListColors.rgb(name).toLong())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    reminder: Reminder,
    lists: List<TaskList>,
    onDismiss: () -> Unit,
    onSave: (
        message: String,
        listId: Long,
        scheduledAt: Long?,
        recurrence: Recurrence,
        url: String,
        onResult: (ReminderViewModel.SaveResult) -> Unit,
    ) -> Unit,
    onToggleCompleted: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit,
) {
    val (initialDate, initialTime) = remember(reminder.id) { initialDateTimeFromReminder(reminder) }
    var message by remember(reminder.id) { mutableStateOf(reminder.message) }
    var listId by remember(reminder.id) { mutableStateOf(reminder.listId) }
    var url by remember(reminder.id) { mutableStateOf(reminder.url) }
    var hasDeadline by remember(reminder.id) { mutableStateOf(reminder.scheduledAt != null) }
    var date by remember(reminder.id) { mutableStateOf(initialDate) }
    var time by remember(reminder.id) { mutableStateOf(initialTime) }
    var recurrence by remember(reminder.id) { mutableStateOf(reminder.recurrenceType) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }

    val completed = ReminderOrdering.isCompleted(reminder, System.currentTimeMillis())
    val dirty = message != reminder.message ||
        url.trim() != reminder.url ||
        listId != reminder.listId ||
        hasDeadline != (reminder.scheduledAt != null) ||
        (hasDeadline && (date != initialDate || time != initialTime)) ||
        (hasDeadline && recurrence != reminder.recurrenceType)
    val dirtyNow by rememberUpdatedState(dirty)

    fun requestClose() {
        if (dirty) confirmDiscard = true else onDismiss()
    }

    fun save(afterSuccess: () -> Unit) {
        val scheduledAt = if (hasDeadline) combineDateAndTime(date, time) else null
        val cleanUrl = ReminderUrl.normalize(url)
        if (cleanUrl == null) {
            error = "URL inválida. Use um endereço http ou https."
            return
        }
        onSave(message, listId, scheduledAt, if (hasDeadline) recurrence else Recurrence.NONE, cleanUrl) { result ->
            when (result) {
                ReminderViewModel.SaveResult.Success -> afterSuccess()
                ReminderViewModel.SaveResult.EmptyMessage -> error = "Digite uma mensagem."
                ReminderViewModel.SaveResult.PastDateTime -> error = "Escolha uma data e horário futuros."
                ReminderViewModel.SaveResult.MissingList -> error = "Escolha uma lista."
                ReminderViewModel.SaveResult.ExactAlarmDenied ->
                    error = "Ative alarmes exatos nas configurações do Android."
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !dirtyNow },
    )
    val accent = listColor(lists, listId)
    val whenLabel = if (hasDeadline) "${dateFmt.format(date)} ${timeFmt.format(time)}" else "Sem prazo"

    ModalBottomSheet(onDismissRequest = { requestClose() }, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = reminder.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { requestClose() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fechar")
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill {
                    Dot(accent)
                    Text(lists.firstOrNull { it.id == listId }?.name.orEmpty(), fontSize = 13.sp)
                }
                Pill {
                    Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(15.dp))
                    Text(whenLabel, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mensagem") },
                shape = RoundedCornerShape(10.dp),
                minLines = 1,
                maxLines = 4,
            )

            Spacer(Modifier.height(12.dp))
            Box {
                Surface(
                    onClick = { listMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Dot(accent)
                        Text(
                            lists.firstOrNull { it.id == listId }?.name ?: "Escolher lista",
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.KeyboardArrowDown, null)
                    }
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            leadingIcon = { Dot(listColor(lists, list.id)) },
                            text = { Text(list.name) },
                            onClick = {
                                listId = list.id
                                listMenu = false
                                error = null
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL (opcional)") },
                placeholder = { Text("https://...") },
                leadingIcon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hasDeadline = !hasDeadline; error = null },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = !hasDeadline, onCheckedChange = { hasDeadline = !it; error = null })
                Column {
                    Text("Sem prazo", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "A tarefa não terá uma data definida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerBox("Data", if (hasDeadline) dateFmt.format(date) else "dd/mm/aaaa", hasDeadline, Modifier.weight(1f)) { showDate = true }
                PickerBox("Horário", if (hasDeadline) timeFmt.format(time) else "--:--", hasDeadline, Modifier.weight(1f)) { showTime = true }
            }

            if (hasDeadline) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Recurrence.entries.forEach { option ->
                        FilterChip(
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                            label = { Text(option.labelPt) },
                        )
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(onClick = { save { onDismiss() } }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar")
                }
                OutlinedButton(
                    onClick = {
                        if (!dirty) {
                            onToggleCompleted()
                            onDismiss()
                        } else {
                            save {
                                if (!completed) onToggleCompleted()
                                onDismiss()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(if (completed) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (completed) "Reabrir" else "Concluir")
                }
                if (reminder.scheduledAt != null && !completed) {
                    OutlinedButton(
                        onClick = { onSnooze(); onDismiss() },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Outlined.Snooze, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adiar 1h")
                    }
                }
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { requestClose() }) { Text("Cancelar") }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
            text = { TimePicker(state = state) },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar alterações?") },
            text = { Text("As alterações não salvas serão perdidas.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") } },
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(color))
}

@Composable
private fun Pill(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) { content() }
    }
}

@Composable
private fun PickerBox(label: String, value: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(10.dp),
            color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
