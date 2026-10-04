package com.vitor.melembre.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.util.DateTimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    reminders: List<Reminder>,
    exactAlarmsAllowed: Boolean,
    onConfigureExactAlarms: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onSnoozeReminder: (Reminder) -> Unit,
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
) {
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                if (!exactAlarmsAllowed) {
                    ExactAlarmBanner(onConfigure = onConfigureExactAlarms)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                EmptyRemindersState(modifier = Modifier.fillMaxSize())
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 0.dp, bottom = 72.dp),
            ) {
                if (!exactAlarmsAllowed) {
                    item(key = "exact_alarm_banner") {
                        ExactAlarmBanner(
                            onConfigure = onConfigureExactAlarms,
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp, top = 2.dp),
                        )
                    }
                }
                itemsIndexed(reminders, key = { _, item -> item.id }) { index, reminder ->
                    CompactReminderRow(
                        reminder = reminder,
                        onClick = { selectedReminder = reminder },
                        onDelete = { onDeleteReminder(reminder) },
                    )
                    if (index < reminders.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }

    selectedReminder?.let { reminder ->
        ModalBottomSheet(
            onDismissRequest = { selectedReminder = null },
            sheetState = sheetState,
        ) {
            ReminderActionsSheet(
                reminder = reminder,
                onSnooze = {
                    onSnoozeReminder(reminder)
                    selectedReminder = null
                },
                onEdit = {
                    onEditReminder(reminder)
                    selectedReminder = null
                },
                onDelete = {
                    onDeleteReminder(reminder)
                    selectedReminder = null
                },
                onCancel = { selectedReminder = null },
            )
        }
    }
}

@Composable
private fun CompactReminderRow(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val completed = reminder.triggered || reminder.scheduledAt <= System.currentTimeMillis()
    val titleColor = if (completed) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val metaColor = if (completed) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val scheduleText = buildString {
        append(DateTimeFormatters.formatReminderSchedule(reminder.scheduledAt))
        reminder.recurrenceType.shortLabelPt?.let { append(" · ").append(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp, max = 60.dp)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                ),
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
            )
            Text(
                text = scheduleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                ),
                color = metaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Excluir lembrete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ReminderActionsSheet(
    reminder: Reminder,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = reminder.message,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = DateTimeFormatters.formatReminderSchedule(reminder.scheduledAt),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )

        SheetActionRow(
            icon = Icons.Outlined.Snooze,
            label = "Adiar 1h",
            onClick = onSnooze,
        )
        SheetActionRow(
            icon = Icons.Outlined.Edit,
            label = "Editar",
            onClick = onEdit,
        )
        SheetActionRow(
            icon = Icons.Outlined.Delete,
            label = "Excluir",
            onClick = onDelete,
        )
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyRemindersState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Nenhum lembrete por enquanto.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Crie um lembrete e eu aviso você na hora certa.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExactAlarmBanner(
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ative alarmes exatos para avisos pontuais.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onConfigure) {
                Text("Ativar")
            }
        }
    }
}
