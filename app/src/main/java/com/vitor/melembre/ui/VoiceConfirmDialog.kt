package com.vitor.melembre.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitor.melembre.util.DateTimeFormatters
import com.vitor.melembre.voice.ParsedVoiceReminder
import java.time.ZoneId

@Composable
fun VoiceConfirmDialog(
    parsed: ParsedVoiceReminder,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    val scheduledAt = combineDateAndTime(parsed.date, parsed.time)
    val whenLabel = DateTimeFormatters.formatReminderSchedule(scheduledAt, ZoneId.systemDefault())
    val recurrenceLabel = parsed.recurrence.shortLabelPt

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirmar lembrete") },
        text = {
            Column {
                Text(
                    text = parsed.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = whenLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (recurrenceLabel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recurrenceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onConfirm) { Text("Confirmar") }
            }
        },
    )
}
