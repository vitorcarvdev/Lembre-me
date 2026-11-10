package com.vitor.melembre.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Link
import androidx.compose.ui.platform.LocalUriHandler
import com.vitor.melembre.data.Recurrence
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.data.TaskList
import com.vitor.melembre.util.DateTimeFormatters
import com.vitor.melembre.util.ListColors
import com.vitor.melembre.util.ReminderOrdering

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    reminders: List<Reminder>,
    lists: List<TaskList>,
    selectedListId: Long?,
    filterReady: Boolean,
    onSelectList: (Long?) -> Unit,
    onCreateList: () -> Unit,
    exactAlarmsAllowed: Boolean,
    onConfigureExactAlarms: () -> Unit,
    onSaveReminder: (
        Reminder,
        String,
        Long,
        Long?,
        Recurrence,
        String,
        (ReminderViewModel.SaveResult) -> Unit,
    ) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onSnoozeReminder: (Reminder) -> Unit,
    onToggleCompleted: (Reminder) -> Unit,
    onReorder: (Long, List<Long>) -> Unit,
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
) {
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    val listState = rememberLazyListState()
    var working by remember { mutableStateOf(reminders) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val canReorder = selectedListId != null
    LaunchedEffect(reminders) { if (draggingId == null) working = reminders }

    fun moveDragged(id: Long, dy: Float) {
        dragOffset += dy
        val infos = listState.layoutInfo.visibleItemsInfo
        val cur = infos.firstOrNull { it.key == id } ?: return
        val curIdx = working.indexOfFirst { it.id == id }
        if (cur.index != curIdx) return
        val center = cur.offset + cur.size / 2f + dragOffset
        val target = infos.firstOrNull { it.key != id && center >= it.offset && center <= it.offset + it.size } ?: return
        val tIdx = working.indexOfFirst { it.id == target.key }
        val t = working.getOrNull(tIdx) ?: return
        if (t.scheduledAt != null || t.triggered) return
        val moved = working.toMutableList()
        moved.add(tIdx, moved.removeAt(curIdx))
        working = moved
        val newOffset = if (tIdx > curIdx) target.offset + target.size - cur.size else target.offset
        dragOffset += (cur.offset - newOffset).toFloat()
    }

    fun finishDrag() {
        val listId = selectedListId
        val ids = working.filter { it.scheduledAt == null && !it.triggered }.map { it.id }
        draggingId = null
        dragOffset = 0f
        if (listId != null) onReorder(listId, ids)
    }

    Scaffold(
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ListFilterBar(
                lists = lists,
                selectedListId = selectedListId,
                filterReady = filterReady,
                onSelectList = onSelectList,
                onCreateList = onCreateList,
            )
            if (!exactAlarmsAllowed) {
                ExactAlarmBanner(
                    onConfigure = onConfigureExactAlarms,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            if (!filterReady) {
                Spacer(modifier = Modifier.weight(1f))
            } else if (reminders.isEmpty()) {
                if (selectedListId == null) {
                    EmptyRemindersState(modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        text = "Nenhuma tarefa nesta lista.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 72.dp),
                ) {
                    itemsIndexed(working, key = { _, item -> item.id }) { index, reminder ->
                        val corner = 16.dp
                        val shape = RoundedCornerShape(
                            topStart = if (index == 0) corner else 0.dp,
                            topEnd = if (index == 0) corner else 0.dp,
                            bottomStart = if (index == working.lastIndex) corner else 0.dp,
                            bottomEnd = if (index == working.lastIndex) corner else 0.dp,
                        )
                        val isDragging = draggingId == reminder.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isDragging) Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset } else Modifier)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            CompactReminderRow(
                                reminder = reminder,
                                listName = lists.firstOrNull { it.id == reminder.listId }?.name.orEmpty(),
                                onClick = { selectedReminder = reminder },
                                onToggle = { onToggleCompleted(reminder) },
                                onDelete = { onDeleteReminder(reminder) },
                                reserveHandle = canReorder,
                                onDragStart = { draggingId = reminder.id; dragOffset = 0f },
                                onDragMove = { dy -> moveDragged(reminder.id, dy) },
                                onDragEnd = { finishDrag() },
                            )
                            if (index < working.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedReminder?.let { reminder ->
        ReminderEditorSheet(
            reminder = reminder,
            lists = lists,
            onDismiss = { selectedReminder = null },
            onSave = { message, listId, scheduledAt, recurrence, url, onResult ->
                onSaveReminder(reminder, message, listId, scheduledAt, recurrence, url, onResult)
            },
            onToggleCompleted = { onToggleCompleted(reminder) },
            onSnooze = { onSnoozeReminder(reminder) },
            onDelete = { onDeleteReminder(reminder) },
        )
    }
}

@Composable
private fun CompactReminderRow(
    reminder: Reminder,
    listName: String,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    reserveHandle: Boolean,
    onDragStart: () -> Unit,
    onDragMove: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val completed = ReminderOrdering.isCompleted(reminder, System.currentTimeMillis())
    val undated = !completed && reminder.scheduledAt == null
    val dark = isSystemInDarkTheme()
    val listColor = Color(
        (0xFF000000L or (if (dark) ListColors.onDark(listName) else ListColors.rgb(listName)).toLong()),
    )
    val titleColor = if (completed) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val metaColor = if (completed || undated) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (completed) 0.75f else 1f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reserveHandle) {
            if (undated) {
                val startCb by rememberUpdatedState(onDragStart)
                val moveCb by rememberUpdatedState(onDragMove)
                val endCb by rememberUpdatedState(onDragEnd)
                Icon(
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = "Arrastar para reordenar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(width = 22.dp, height = 36.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startCb() },
                                onDragEnd = { endCb() },
                                onDragCancel = { endCb() },
                            ) { change, amount ->
                                change.consume()
                                moveCb(amount.y)
                            }
                        },
                )
            } else {
                Spacer(modifier = Modifier.width(22.dp))
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (completed) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Reabrir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (completed) FontWeight.Normal else FontWeight.Medium,
                ),
                color = titleColor,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!reserveHandle) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(listColor),
                    )
                } else if (!undated) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = metaColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Text(
                    text = scheduleLabel(reminder),
                    modifier = Modifier.padding(start = if (!reserveHandle || !undated) 5.dp else 0.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                    ),
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (reminder.url.isNotBlank()) {
            val uriHandler = LocalUriHandler.current
            IconButton(
                onClick = { runCatching { uriHandler.openUri(reminder.url) } },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = "Abrir link",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Excluir lembrete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
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

@Composable
private fun ListFilterBar(
    lists: List<TaskList>,
    selectedListId: Long?,
    filterReady: Boolean,
    onSelectList: (Long?) -> Unit,
    onCreateList: () -> Unit,
) {
    val current = lists.firstOrNull { it.id == selectedListId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (current != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF000000L or ListColors.rgb(current.name).toLong())),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = if (!filterReady || current == null) "Todas" else current.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun FilterChipLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color? = null,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (selected) Modifier else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .then(if (selected) Modifier.border(1.5.dp, Color.White, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(dot),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        )
    }
}

private fun scheduleLabel(reminder: Reminder): String {
    val scheduledAt = reminder.scheduledAt ?: return "Sem prazo"
    return buildString {
        append(DateTimeFormatters.formatReminderSchedule(scheduledAt))
        reminder.recurrenceType.shortLabelPt?.let { append(" · ").append(it) }
    }
}
