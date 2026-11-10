package com.vitor.melembre.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vitor.melembre.focus.FocusBlock
import com.vitor.melembre.focus.FocusChime
import com.vitor.melembre.focus.FocusMode
import com.vitor.melembre.focus.FocusPhase
import com.vitor.melembre.focus.FocusSchedule
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

private val ClockColor = Color(0xFFB8B6C6)
private val SoftColor = Color(0xFF8F8D9F)
private val NameColor = Color(0xFFD2D0DE)
private val WeekDays = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
private val Months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

private fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun FocusScreen(agenda: List<FocusBlock>, onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        FocusMode.active = true
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.also { it.screenBrightness = 0.18f }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            FocusMode.active = false
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.attributes = window.attributes.also {
                    it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(onBack = onExit)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L + 5L)
        }
    }
    val local = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val moment = FocusSchedule.momentAt(agenda, local.toLocalTime().toSecondOfDay())
    val next = FocusSchedule.nextBlock(agenda, local.toLocalTime().toSecondOfDay())

    var battery by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) battery = level * 100 / scale
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        receiver.onReceive(context, sticky)
        onDispose { context.unregisterReceiver(receiver) }
    }

    var lastKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(moment.key) {
        if (lastKey != null && lastKey != moment.key) FocusChime.play()
        lastKey = moment.key
    }

    var controls by remember { mutableStateOf(false) }
    LaunchedEffect(controls) {
        if (controls) {
            delay(5000)
            controls = false
        }
    }

    val scenes = when (moment.phase) {
        FocusPhase.FOCUS -> FocusScenes.focus
        FocusPhase.BREAK -> FocusScenes.breaks
        FocusPhase.REST -> FocusScenes.rest
        FocusPhase.IDLE -> emptyList()
    }
    val sceneId = if (scenes.isEmpty()) "" else scenes[((now / 240_000L) % scenes.size).toInt()]
    val shift = ((now / 60_000L) % 9).toInt() - 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                controls = !controls
            },
    ) {
        Crossfade(targetState = sceneId, animationSpec = tween(3000), label = "focus-scene") { id ->
            if (id.isNotEmpty()) {
                FocusScene(id)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)))
            }
        }

        Box(Modifier.fillMaxSize().offset(x = (shift * 2).dp, y = (-shift).dp)) {
            if (moment.phase != FocusPhase.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    agenda.forEachIndexed { i, _ ->
                        val color = when {
                            i == moment.blockIndex -> Color(0xFF8E88C4)
                            i < moment.blockIndex -> Color(0xFF2C2A3A)
                            else -> Color(0xFF1C1B24)
                        }
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    }
                }
                Column(Modifier.align(Alignment.TopStart).padding(start = 26.dp, top = 34.dp)) {
                    Text(moment.block?.name.orEmpty(), color = NameColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "até ${FocusSchedule.formatMinutes(moment.block?.endMin ?: 0)}",
                        color = SoftColor,
                        fontSize = 13.sp,
                    )
                }
                if (moment.phase != FocusPhase.FOCUS) {
                    Text(
                        "Beba água",
                        modifier = Modifier.align(Alignment.Center),
                        color = NameColor.copy(alpha = 0.85f),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 22.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        "%02d:%02d".format(moment.remainingSec / 60, moment.remainingSec % 60),
                        color = ClockColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        when (moment.phase) {
                            FocusPhase.FOCUS -> "foco · ciclo ${moment.cycle} de ${moment.cycles}"
                            FocusPhase.BREAK -> "pausa"
                            else -> "intervalo"
                        },
                        color = SoftColor,
                        fontSize = 13.sp,
                    )
                }
            }
            if (next != null) {
                Text(
                    "Próxima: ${FocusSchedule.formatMinutes(next.startMin)} · ${next.name}",
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(start = 26.dp, top = if (moment.phase == FocusPhase.IDLE) 20.dp else 80.dp),
                    color = SoftColor,
                    fontSize = 14.sp,
                )
            }
            if (battery >= 0) {
                Text(
                    (if (charging) "⚡ " else "") + "$battery%",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
                    color = if (battery <= 15 && !charging) Color(0xFFC98A7E) else SoftColor,
                    fontSize = 14.sp,
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 26.dp, bottom = 22.dp)) {
                Text(
                    "%02d:%02d".format(local.hour, local.minute),
                    color = ClockColor,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${WeekDays[local.dayOfWeek.value - 1]}, ${local.dayOfMonth} ${Months[local.monthValue - 1]}",
                    color = SoftColor,
                    fontSize = 14.sp,
                )
            }
            if (moment.phase != FocusPhase.IDLE) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0xFF17171A))) {
                    Box(
                        Modifier
                            .fillMaxWidth(moment.blockFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(Color(0xFF6A6592)),
                    )
                }
            }
        }

        if (controls) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 26.dp),
            ) {
                Text("Sair do foco")
            }
        }
    }
}
