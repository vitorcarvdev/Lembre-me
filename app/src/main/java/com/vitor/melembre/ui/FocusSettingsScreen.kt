package com.vitor.melembre.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitor.melembre.focus.FocusBlock
import com.vitor.melembre.focus.FocusSchedule

private class AgendaRow(name: String, start: String, end: String, rest: Boolean) {
    var name by mutableStateOf(name)
    var start by mutableStateOf(start)
    var end by mutableStateOf(end)
    var rest by mutableStateOf(rest)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSettingsScreen(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    agenda: List<FocusBlock>,
    onSaveAgenda: (List<FocusBlock>) -> Unit,
    onOpenNow: () -> Unit,
    onBack: () -> Unit,
) {
    val rows = remember(agenda) {
        mutableStateListOf<AgendaRow>().apply {
            agenda.forEach {
                add(AgendaRow(it.name, FocusSchedule.formatMinutes(it.startMin), FocusSchedule.formatMinutes(it.endMin), it.rest))
            }
        }
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo foco") },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ativar modo foco neste aparelho", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use no celular de mesa. Com o acesso pelo computador ativo, a tela de foco abre sozinha. Os lembretes ficam em silêncio enquanto ela estiver aberta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenNow, shape = RoundedCornerShape(10.dp)) {
                Text("Abrir tela de foco agora")
            }

            Spacer(Modifier.height(24.dp))
            Text("Agenda do dia", style = MaterialTheme.typography.titleMedium)
            Text(
                "Dentro de cada bloco: ${FocusSchedule.FOCUS_MIN} min de foco e ${FocusSchedule.BREAK_MIN} de pausa. Blocos de intervalo (como o almoço) não têm ciclos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            rows.forEachIndexed { index, row ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = row.name,
                        onValueChange = { row.name = it; error = null; saved = false },
                        label = { Text("Nome do bloco") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = row.start,
                            onValueChange = { row.start = it; error = null; saved = false },
                            label = { Text("Início") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                        )
                        OutlinedTextField(
                            value = row.end,
                            onValueChange = { row.end = it; error = null; saved = false },
                            label = { Text("Fim") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                        )
                        Checkbox(checked = row.rest, onCheckedChange = { row.rest = it; saved = false })
                        Text("Intervalo", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { rows.removeAt(index); error = null; saved = false }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remover bloco", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }

            TextButton(onClick = {
                val last = rows.lastOrNull()?.end?.let { FocusSchedule.parseMinutes(it) } ?: (8 * 60)
                val end = minOf(last + 60, 24 * 60)
                rows.add(AgendaRow("", FocusSchedule.formatMinutes(last), FocusSchedule.formatMinutes(end), false))
                saved = false
            }) {
                Text("Adicionar bloco")
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            if (saved) {
                Text("Agenda salva.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    val blocks = mutableListOf<FocusBlock>()
                    for (row in rows) {
                        val start = FocusSchedule.parseMinutes(row.start)
                        val end = FocusSchedule.parseMinutes(row.end)
                        if (start == null || end == null) {
                            error = "Use horários no formato 08:30."
                            return@Button
                        }
                        blocks += FocusBlock(FocusSchedule.cleanName(row.name), start, end, row.rest)
                    }
                    val problem = FocusSchedule.validate(blocks)
                    if (problem != null) {
                        error = problem
                    } else {
                        error = null
                        onSaveAgenda(blocks.sortedBy { it.startMin })
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Salvar agenda")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
