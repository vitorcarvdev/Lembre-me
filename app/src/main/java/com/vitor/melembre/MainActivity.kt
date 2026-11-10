package com.vitor.melembre

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.vitor.melembre.util.ListColors
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitor.melembre.alarm.ReminderScheduler
import com.vitor.melembre.data.Recurrence
import com.vitor.melembre.focus.FocusStore
import com.vitor.melembre.ui.FocusScreen
import com.vitor.melembre.ui.FocusSettingsScreen
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.localweb.LocalAccessController
import com.vitor.melembre.localweb.LocalAccessService
import com.vitor.melembre.localweb.LocalIp
import com.vitor.melembre.localweb.LocalPin
import com.vitor.melembre.ui.CreateListDialog
import com.vitor.melembre.ui.HomeScreen
import com.vitor.melembre.ui.ManageListsScreen
import com.vitor.melembre.ui.LocalAccessDialog
import com.vitor.melembre.ui.ReminderFormScreen
import com.vitor.melembre.ui.ReminderViewModel
import com.vitor.melembre.ui.VoiceConfirmDialog
import com.vitor.melembre.ui.combineDateAndTime
import com.vitor.melembre.ui.defaultListId
import com.vitor.melembre.ui.initialDateTimeFromReminder
import com.vitor.melembre.ui.theme.MeLembreTheme
import com.vitor.melembre.voice.ParsedVoiceReminder
import com.vitor.melembre.voice.VoicePhraseParser
import com.vitor.melembre.voice.VoiceRecognitionHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    private val viewModel: ReminderViewModel by viewModels()

    private val voiceTrigger = mutableIntStateOf(0)
    private val voiceParsed = mutableStateOf<ParsedVoiceReminder?>(null)
    private val voiceError = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchSpeechRecognition()
            } else {
                voiceError.value = getString(R.string.voice_permission_needed)
            }
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val spoken = VoiceRecognitionHelper.extractBestResult(result.data)
            if (spoken.isNullOrBlank()) {
                voiceError.value = getString(R.string.voice_not_understood)
                return@registerForActivityResult
            }
            val parsed = VoicePhraseParser.parse(spoken)
            if (parsed == null) {
                voiceError.value = getString(R.string.voice_not_understood)
            } else {
                voiceParsed.value = parsed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (savedInstanceState == null && shouldStartVoice(intent)) {
            voiceTrigger.intValue += 1
            intent.action = Intent.ACTION_MAIN
        }

        setContent {
            MeLembreTheme {
                MeLembreAppContent(
                    viewModel = viewModel,
                    voiceTrigger = voiceTrigger.intValue,
                    voiceParsed = voiceParsed.value,
                    voiceError = voiceError.value,
                    onConsumeVoiceError = { voiceError.value = null },
                    onRequestVoice = { startVoiceReminderFlow() },
                    onConfirmVoice = { parsed ->
                        val scheduledAt = combineDateAndTime(parsed.date, parsed.time)
                        viewModel.saveReminder(
                            existing = null,
                            message = parsed.message,
                            listId = null,
                            scheduledAt = scheduledAt,
                            recurrence = parsed.recurrence,
                        ) { result ->
                            when (result) {
                                ReminderViewModel.SaveResult.Success -> {
                                    voiceParsed.value = null
                                }
                                ReminderViewModel.SaveResult.PastDateTime -> {
                                    voiceError.value = "Escolha uma data e horário futuros."
                                }
                                ReminderViewModel.SaveResult.ExactAlarmDenied -> {
                                    voiceError.value = "Ative alarmes exatos nas configurações do Android."
                                    openExactAlarmSettings()
                                }
                                ReminderViewModel.SaveResult.EmptyMessage -> {
                                    voiceError.value = getString(R.string.voice_not_understood)
                                }
                                ReminderViewModel.SaveResult.MissingList -> {
                                    voiceError.value = "Não foi possível salvar o lembrete."
                                }
                            }
                        }
                    },
                    onEditVoice = { voiceParsed.value = null },
                    onCancelVoice = { voiceParsed.value = null },
                    onRequestExactAlarmSettings = { openExactAlarmSettings() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (shouldStartVoice(intent)) {
            voiceTrigger.intValue += 1
            intent.action = Intent.ACTION_MAIN
            setIntent(intent)
        }
    }

    private fun shouldStartVoice(intent: Intent?): Boolean {
        return intent?.action == VoiceRecognitionHelper.ACTION_START_VOICE_REMINDER
    }

    fun startVoiceReminderFlow() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> launchSpeechRecognition()
            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognition() {
        val speechIntent = VoiceRecognitionHelper.createSpeechIntent()
        if (speechIntent.resolveActivity(packageManager) == null) {
            // Em Android 11+ resolveActivity pode falhar sem <queries>; ainda tentamos lançar.
        }
        try {
            speechLauncher.launch(speechIntent)
        } catch (_: Exception) {
            voiceError.value = getString(R.string.voice_unavailable)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

private sealed class AppScreen {
    data object Home : AppScreen()
    data class Form(val reminder: Reminder?) : AppScreen()
    data object ManageLists : AppScreen()
    data object Focus : AppScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeLembreAppContent(
    viewModel: ReminderViewModel,
    voiceTrigger: Int,
    voiceParsed: ParsedVoiceReminder?,
    voiceError: String?,
    onConsumeVoiceError: () -> Unit,
    onRequestVoice: () -> Unit,
    onConfirmVoice: (ParsedVoiceReminder) -> Unit,
    onEditVoice: (ParsedVoiceReminder) -> Unit,
    onCancelVoice: () -> Unit,
    onRequestExactAlarmSettings: () -> Unit,
) {
    val reminders by viewModel.homeReminders.collectAsStateWithLifecycle()
    val taskLists by viewModel.taskLists.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val openCounts by viewModel.openCounts.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val filterReady by viewModel.filterReady.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var formMessage by remember { mutableStateOf("") }
    var formListId by remember { mutableStateOf<Long?>(null) }
    var formHasDeadline by remember { mutableStateOf(false) }
    var formDate by remember { mutableStateOf(LocalDate.now()) }
    var formTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var formRecurrence by remember { mutableStateOf(Recurrence.NONE) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCreateList by remember { mutableStateOf(false) }
    var listError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<com.vitor.melembre.data.TaskList?>(null) }
    var pendingDeleteCount by remember { mutableIntStateOf(0) }
    var exactAlarmHintShown by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<ParsedVoiceReminder?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exactAlarmsAllowed = ReminderScheduler.canScheduleExactAlarms(context)
    var showComputerAccess by remember { mutableStateOf(false) }
    var savedPin by remember { mutableStateOf(LocalPin.read(context)) }
    val computerAccess by LocalAccessController.state.collectAsStateWithLifecycle()
    val focusStore = remember { FocusStore(context) }
    var focusEnabled by remember { mutableStateOf(focusStore.isEnabled()) }
    var focusOpen by rememberSaveable { mutableStateOf(false) }
    // O foco só abre sozinho quando o acesso pelo computador é ligado agora (não ao abrir o app,
    // nem ao girar a tela, nem depois de o usuário sair do foco).
    var lastAutoFocus by remember { mutableStateOf(computerAccess.active && focusEnabled) }

    LaunchedEffect(computerAccess.active, focusEnabled) {
        val want = computerAccess.active && focusEnabled
        if (want && !lastAutoFocus) focusOpen = true
        if (!computerAccess.active) focusOpen = false
        lastAutoFocus = want
    }

    LaunchedEffect(voiceTrigger) {
        if (voiceTrigger > 0) {
            onRequestVoice()
        }
    }

    LaunchedEffect(voiceParsed) {
        pendingConfirm = voiceParsed
    }

    LaunchedEffect(voiceError) {
        val message = voiceError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onConsumeVoiceError()
    }

    LaunchedEffect(exactAlarmsAllowed, exactAlarmHintShown) {
        if (!exactAlarmsAllowed && !exactAlarmHintShown) {
            exactAlarmHintShown = true
            val result = snackbarHostState.showSnackbar(
                message = "Permita alarmes exatos para receber lembretes na hora certa.",
                actionLabel = "Configurar",
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRequestExactAlarmSettings()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        when (val current = screen) {
            AppScreen.Home -> {
                BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                            Text(
                                text = "Lembre-me",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp),
                            )
                            val totalOpen = openCounts.values.sum()
                            DrawerEntry(
                                label = "Todas",
                                selected = filterReady && selectedListId == null,
                                count = totalOpen,
                                leading = { Icon(Icons.Filled.Inbox, contentDescription = null) },
                            ) {
                                viewModel.selectList(null)
                                scope.launch { drawerState.close() }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 6.dp))
                            taskLists.forEach { list ->
                                DrawerEntry(
                                    label = list.name,
                                    selected = selectedListId == list.id,
                                    count = openCounts[list.id] ?: 0,
                                    leading = {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF000000L or ListColors.rgb(list.name).toLong())),
                                        )
                                    },
                                ) {
                                    viewModel.selectList(list.id)
                                    scope.launch { drawerState.close() }
                                }
                            }
                            DrawerEntry(
                                label = "Nova lista",
                                selected = false,
                                count = null,
                                leading = { Icon(Icons.Filled.Add, contentDescription = null) },
                            ) {
                                scope.launch { drawerState.close() }
                                listError = null
                                showCreateList = true
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 6.dp))
                            DrawerEntry(
                                label = "Gerenciar listas",
                                selected = false,
                                count = null,
                                leading = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            ) {
                                scope.launch { drawerState.close() }
                                screen = AppScreen.ManageLists
                            }
                            DrawerEntry(
                                label = "Modo foco",
                                selected = false,
                                count = null,
                                leading = { Icon(Icons.Filled.Timer, contentDescription = null) },
                            ) {
                                scope.launch { drawerState.close() }
                                screen = AppScreen.Focus
                            }
                            DrawerEntry(
                                label = stringResource(R.string.computer_access),
                                selected = false,
                                count = null,
                                leading = { Icon(Icons.Filled.Computer, contentDescription = null) },
                            ) {
                                scope.launch { drawerState.close() }
                                showComputerAccess = true
                            }
                        }
                    },
                ) {
                HomeScreen(
                    reminders = reminders,
                    lists = taskLists,
                    selectedListId = selectedListId,
                    filterReady = filterReady,
                    onSelectList = viewModel::selectList,
                    onCreateList = {
                        listError = null
                        showCreateList = true
                    },
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    onConfigureExactAlarms = onRequestExactAlarmSettings,
                    onSaveReminder = { reminder, message, listId, scheduledAt, recurrence, url, onResult ->
                        viewModel.saveReminder(
                            existing = reminder,
                            message = message,
                            listId = listId,
                            scheduledAt = scheduledAt,
                            recurrence = recurrence,
                            onResult = onResult,
                            url = url,
                        )
                    },
                    onDeleteReminder = viewModel::deleteReminder,
                    onReorder = viewModel::reorder,
                    onToggleCompleted = { reminder ->
                        viewModel.toggleCompleted(reminder) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message = message) }
                        }
                    },
                    onSnoozeReminder = { reminder ->
                        viewModel.snoozeReminderOneHour(reminder) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Ative alarmes exatos para adiar o lembrete.",
                                    actionLabel = "Abrir",
                                ).let { result ->
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRequestExactAlarmSettings()
                                    }
                                }
                            }
                        }
                    },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { HomeHeader(compact = reminders.isNotEmpty()) },
                            actions = {
                                IconButton(onClick = { focusOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Entrar no modo foco",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { showComputerAccess = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Computer,
                                        contentDescription = stringResource(R.string.computer_access),
                                        tint = if (computerAccess.active) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Abrir menu",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                            expandedHeight = if (reminders.isNotEmpty()) 48.dp else TopAppBarDefaults.TopAppBarExpandedHeight,
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                formListId = defaultListId(taskLists, selectedListId)
                                formHasDeadline = false
                                openNewReminderForm(
                                    onMessage = { formMessage = it },
                                    onDate = { formDate = it },
                                    onTime = { formTime = it },
                                    onRecurrence = { formRecurrence = it },
                                    onErrorClear = { formError = null },
                                    onScreen = { screen = it },
                                )
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.new_reminder),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    },
                )
                }
            }

            is AppScreen.Form -> {
                ReminderFormScreen(
                    existing = current.reminder,
                    message = formMessage,
                    onMessageChange = {
                        formMessage = it
                        formError = null
                    },
                    lists = taskLists,
                    selectedListId = formListId,
                    onListSelected = {
                        formListId = it
                        formError = null
                    },
                    hasDeadline = formHasDeadline,
                    onHasDeadlineChange = {
                        formHasDeadline = it
                        formError = null
                        if (!it) formRecurrence = Recurrence.NONE
                    },
                    selectedDate = formDate,
                    onDateClick = { showDatePicker = true },
                    selectedTime = formTime,
                    onTimeClick = { showTimePicker = true },
                    recurrence = formRecurrence,
                    onRecurrenceChange = { formRecurrence = it },
                    onSave = {
                        val scheduledAt = if (formHasDeadline) {
                            combineDateAndTime(formDate, formTime)
                        } else {
                            null
                        }
                        viewModel.saveReminder(
                            existing = current.reminder,
                            message = formMessage,
                            listId = formListId,
                            scheduledAt = scheduledAt,
                            recurrence = if (formHasDeadline) formRecurrence else Recurrence.NONE,
                        ) { result ->
                            when (result) {
                                ReminderViewModel.SaveResult.Success -> {
                                    formError = null
                                    screen = AppScreen.Home
                                }
                                ReminderViewModel.SaveResult.EmptyMessage -> {
                                    formError = "Digite uma mensagem."
                                }
                                ReminderViewModel.SaveResult.PastDateTime -> {
                                    formError = "Escolha uma data e horário futuros."
                                }
                                ReminderViewModel.SaveResult.MissingList -> {
                                    formError = "Escolha uma lista."
                                }
                                ReminderViewModel.SaveResult.ExactAlarmDenied -> {
                                    formError = "Ative alarmes exatos nas configurações do Android."
                                    scope.launch {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = "Alarmes exatos desativados.",
                                            actionLabel = "Abrir",
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            onRequestExactAlarmSettings()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBack = { screen = AppScreen.Home },
                    errorMessage = formError,
                )
            }

            AppScreen.Focus -> {
                val focusAgenda = remember { focusStore.load() }
                FocusSettingsScreen(
                    enabled = focusEnabled,
                    onEnabledChange = {
                        focusEnabled = it
                        focusStore.setEnabled(it)
                    },
                    agenda = focusAgenda,
                    onSaveAgenda = { focusStore.save(it) },
                    onOpenNow = { focusOpen = true },
                    onBack = { screen = AppScreen.Home },
                )
            }

            AppScreen.ManageLists -> {
                ManageListsScreen(
                    lists = taskLists,
                    onBack = { screen = AppScreen.Home },
                    onRename = { list, name ->
                        viewModel.renameList(list.id, name) { result ->
                            listError = listResultMessage(result)
                            if (listError != null) {
                                scope.launch { snackbarHostState.showSnackbar(listError!!) }
                            }
                        }
                    },
                    onRequestDelete = { list ->
                        viewModel.countTasks(list.id) { count ->
                            pendingDeleteCount = count
                            pendingDelete = list
                        }
                    },
                )
            }
        }
    }

    if (focusOpen) {
        val agendaNow = remember { focusStore.load() }
        FocusScreen(agenda = agendaNow, onExit = { focusOpen = false })
    }

    pendingConfirm?.let { parsed ->
        VoiceConfirmDialog(
            parsed = parsed,
            onConfirm = { onConfirmVoice(parsed) },
            onEdit = {
                onEditVoice(parsed)
                formMessage = parsed.message
                formDate = parsed.date
                formTime = parsed.time
                formHasDeadline = true
                formListId = defaultListId(taskLists, null)
                formRecurrence = parsed.recurrence
                formError = null
                pendingConfirm = null
                screen = AppScreen.Form(null)
            },
            onCancel = {
                pendingConfirm = null
                onCancelVoice()
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formDate
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            formDate = Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = formTime.hour,
            initialMinute = formTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        formTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    if (showComputerAccess) {
        LocalAccessDialog(
            state = computerAccess,
            pin = savedPin,
            onDismiss = { showComputerAccess = false },
            onActivate = {
                if (computerAccess.active || computerAccess.starting) return@LocalAccessDialog
                val ip = LocalIp.wifiIpv4(context)
                if (ip == null) {
                    LocalAccessController.markUnavailable()
                } else {
                    val pin = LocalPin.read(context)
                    savedPin = pin
                    LocalAccessController.markStarting()
                    LocalAccessService.start(context, ip, pin)
                }
            },
            onDeactivate = { LocalAccessService.stop(context) },
            onChangePin = { newPin ->
                val saved = LocalPin.save(context, newPin)
                if (saved) {
                    savedPin = LocalPin.read(context)
                    if (computerAccess.active) {
                        val ip = LocalIp.wifiIpv4(context)
                        if (ip != null) {
                            LocalAccessService.start(context, ip, savedPin)
                        }
                    }
                }
                saved
            },
        )
    }

    if (showCreateList) {
        CreateListDialog(
            onDismiss = { showCreateList = false },
            onCreate = { name ->
                viewModel.createList(name) { result ->
                    when (result) {
                        is ReminderViewModel.ListSaveResult.Success -> {
                            showCreateList = false
                            viewModel.selectList(result.id)
                        }
                        else -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(listResultMessage(result) ?: "Não foi possível criar a lista.")
                            }
                        }
                    }
                }
            },
        )
    }

    pendingDelete?.let { list ->
        val message = if (pendingDeleteCount == 0) {
            "Excluir a lista ${list.name}?"
        } else {
            "Esta lista possui $pendingDeleteCount tarefas. Elas serão movidas para Lembretes."
        }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir lista") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list.id) { result ->
                        if (result !is ReminderViewModel.ListSaveResult.Success) {
                            scope.launch {
                                snackbarHostState.showSnackbar(listResultMessage(result) ?: "Não foi possível excluir a lista.")
                            }
                        }
                    }
                    pendingDelete = null
                }) {
                    Text("Excluir lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

private fun listResultMessage(result: ReminderViewModel.ListSaveResult): String? {
    return when (result) {
        is ReminderViewModel.ListSaveResult.Success -> null
        ReminderViewModel.ListSaveResult.EmptyName -> "Digite um nome."
        ReminderViewModel.ListSaveResult.Duplicate -> "Já existe uma lista com esse nome."
        ReminderViewModel.ListSaveResult.Protected -> "Esta lista não pode ser alterada."
        ReminderViewModel.ListSaveResult.Missing -> "Não foi possível salvar a lista."
    }
}

private fun openNewReminderForm(
    onMessage: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    onRecurrence: (Recurrence) -> Unit,
    onErrorClear: () -> Unit,
    onScreen: (AppScreen) -> Unit,
) {
    onMessage("")
    val (date, time) = initialDateTimeFromReminder(null)
    onDate(date)
    onTime(time)
    onRecurrence(Recurrence.NONE)
    onErrorClear()
    onScreen(AppScreen.Form(null))
}

@Composable
private fun DrawerEntry(
    label: String,
    selected: Boolean,
    count: Int?,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = leading,
        badge = if (count != null && count > 0) {
            { Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            null
        },
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun HomeHeader(compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.app_name),
            style = if (compact) {
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                )
            } else {
                MaterialTheme.typography.headlineLarge
            },
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!compact) {
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
