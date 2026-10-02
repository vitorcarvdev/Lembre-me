package com.vitor.melembre.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.data.Reminder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderFormScreen(
    existing: Reminder?,
    message: String,
    onMessageChange: (String) -> Unit,
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    selectedTime: LocalTime,
    onTimeClick: () -> Unit,
    recurrence: Recurrence,
    onRecurrenceChange: (Recurrence) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String?,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (existing == null) "Novo lembrete" else "Editar lembrete")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mensagem") },
                placeholder = { Text("O que devo lembrar?") },
                singleLine = false,
                minLines = 2,
            )

            Spacer(modifier = Modifier.height(20.dp))

            FormPickerField(
                label = "Data",
                value = dateFormatter.format(selectedDate),
                onClick = onDateClick,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FormPickerField(
                label = "Hora",
                value = timeFormatter.format(selectedTime),
                onClick = onTimeClick,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Repetir",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Recurrence.entries.forEach { option ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { onRecurrenceChange(option) },
                        label = { Text(option.labelPt) },
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar lembrete")
            }
        }
    }
}

@Composable
private fun FormPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

fun combineDateAndTime(date: LocalDate, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
}

fun initialDateTimeFromReminder(reminder: Reminder?, zoneId: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalTime> {
    if (reminder == null) {
        val now = Instant.now().atZone(zoneId)
        return now.toLocalDate() to now.toLocalTime().withSecond(0).withNano(0)
    }
    val zdt = Instant.ofEpochMilli(reminder.scheduledAt).atZone(zoneId)
    return zdt.toLocalDate() to zdt.toLocalTime()
}
