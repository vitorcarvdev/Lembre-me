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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.localweb.LocalAccessController
import com.vitor.melembre.localweb.LocalAccessService
import com.vitor.melembre.localweb.LocalIp
import com.vitor.melembre.localweb.LocalPin
import com.vitor.melembre.shortcut.VoiceShortcutHelper
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
                    onEditReminder = { reminder ->
                        formMessage = reminder.message
                        formListId = reminder.listId
                        formHasDeadline = reminder.scheduledAt != null
                        val (date, time) = initialDateTimeFromReminder(reminder)
                        formDate = date
                        formTime = time
                        formRecurrence = reminder.recurrenceType
                        formError = null
                        screen = AppScreen.Form(reminder)
                    },
                    onDeleteReminder = viewModel::deleteReminder,
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
                                IconButton(onClick = onRequestVoice) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = stringResource(R.string.voice_reminder),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                VoiceShortcutMenu(
                                    onComputerAccess = { showComputerAccess = true },
                                    onManageLists = { screen = AppScreen.ManageLists },
                                    onPinResult = { result ->
                                        val message = when (result) {
                                            VoiceShortcutHelper.PinResult.AlreadyPinned ->
                                                context.getString(R.string.voice_shortcut_already)
                                            VoiceShortcutHelper.PinResult.Unsupported ->
                                                context.getString(R.string.voice_shortcut_unsupported)
                                            VoiceShortcutHelper.PinResult.Requested -> null
                                        }
                                        if (message != null) {
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        }
                                    },
                                )
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
private fun VoiceShortcutMenu(
    onComputerAccess: () -> Unit,
    onManageLists: () -> Unit,
    onPinResult: (VoiceShortcutHelper.PinResult) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.voice_shortcut_add)) },
                onClick = {
                    expanded = false
                    onPinResult(VoiceShortcutHelper.requestPin(context))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manage_lists)) },
                onClick = {
                    expanded = false
                    onManageLists()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.computer_access)) },
                onClick = {
                    expanded = false
                    onComputerAccess()
                },
            )
        }
    }
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
