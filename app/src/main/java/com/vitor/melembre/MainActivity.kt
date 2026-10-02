package com.vitor.melembre

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitor.melembre.alarm.ReminderScheduler
import com.vitor.melembre.data.Reminder
import com.vitor.melembre.ui.HomeScreen
import com.vitor.melembre.ui.ReminderFormScreen
import com.vitor.melembre.ui.ReminderViewModel
import com.vitor.melembre.ui.combineDateAndTime
import com.vitor.melembre.ui.initialDateTimeFromReminder
import com.vitor.melembre.ui.theme.MeLembreTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
class MainActivity : ComponentActivity() {
    private val viewModel: ReminderViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MeLembreTheme {
                MeLembreAppContent(
                    viewModel = viewModel,
                    onRequestExactAlarmSettings = { openExactAlarmSettings() },
                )
            }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeLembreAppContent(
    viewModel: ReminderViewModel,
    onRequestExactAlarmSettings: () -> Unit,
) {
    val reminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var formMessage by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf(LocalDate.now()) }
    var formTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var exactAlarmHintShown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exactAlarmsAllowed = ReminderScheduler.canScheduleExactAlarms(context)

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
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    onConfigureExactAlarms = onRequestExactAlarmSettings,
                    onEditReminder = { reminder ->
                        formMessage = reminder.message
                        val (date, time) = initialDateTimeFromReminder(reminder)
                        formDate = date
                        formTime = time
                        formError = null
                        screen = AppScreen.Form(reminder)
                    },
                    onDeleteReminder = viewModel::deleteReminder,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { HomeHeader() },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                openNewReminderForm(
                                    onMessage = { formMessage = it },
                                    onDate = { formDate = it },
                                    onTime = { formTime = it },
                                    onErrorClear = { formError = null },
                                    onScreen = { screen = it },
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_reminder))
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
                    selectedDate = formDate,
                    onDateClick = { showDatePicker = true },
                    selectedTime = formTime,
                    onTimeClick = { showTimePicker = true },
                    onSave = {
                        val scheduledAt = combineDateAndTime(formDate, formTime)
                        viewModel.saveReminder(current.reminder, formMessage, scheduledAt) { result ->
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
        }
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
                            // DatePicker retorna meia-noite UTC da data selecionada.
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
}

private fun openNewReminderForm(
    onMessage: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    onErrorClear: () -> Unit,
    onScreen: (AppScreen) -> Unit,
) {
    onMessage("")
    val (date, time) = initialDateTimeFromReminder(null)
    onDate(date)
    onTime(time)
    onErrorClear()
    onScreen(AppScreen.Form(null))
}

@Composable
private fun HomeHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
