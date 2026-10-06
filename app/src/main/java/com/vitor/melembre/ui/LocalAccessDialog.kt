package com.vitor.melembre.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitor.melembre.localweb.LocalAccessController
import com.vitor.melembre.localweb.LocalAccessSnapshot
import com.vitor.melembre.localweb.LocalPin

@Composable
fun LocalAccessDialog(
    state: LocalAccessSnapshot,
    pin: String,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onChangePin: (String) -> Boolean,
) {
    val status = when {
        state.active -> "Ativo"
        state.starting -> "Ativando"
        else -> "Desativado"
    }
    var showChangePin by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Acesso pelo computador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status: $status")
                if (state.active && !state.url.isNullOrBlank()) {
                    Text("Abra no computador:")
                    SelectionContainer {
                        Text(
                            text = state.url,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Text("PIN:")
                SelectionContainer {
                    Text(
                        text = pin,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 6.sp),
                    )
                }
                TextButton(onClick = { showChangePin = true }) {
                    Text("Alterar PIN")
                }
                if (state.active) {
                    Text(
                        text = LocalAccessController.SAME_WIFI_MESSAGE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.detail.isNullOrBlank()) {
                    Text(
                        text = state.detail,
                        color = if (state.detail == LocalAccessController.NO_WIFI_MESSAGE) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (state.active) {
                TextButton(onClick = onDeactivate) {
                    Text("Desativar acesso")
                }
            } else {
                TextButton(
                    onClick = onActivate,
                    enabled = !state.starting,
                ) {
                    Text("Ativar acesso")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
    )

    if (showChangePin) {
        ChangePinDialog(
            currentPin = pin,
            onDismiss = { showChangePin = false },
            onSave = { newPin ->
                if (onChangePin(newPin)) {
                    showChangePin = false
                }
            },
        )
    }
}

@Composable
private fun ChangePinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PIN atual:")
                Text(
                    text = currentPin,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 4.sp),
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it.filter { char -> char in '0'..'9' }.take(4)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Novo PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = {
                        confirmation = it.filter { char -> char in '0'..'9' }.take(4)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirmar PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        !LocalPin.isValid(draft) -> {
                            error = "O PIN deve ter exatamente 4 dígitos."
                        }
                        draft != confirmation -> {
                            error = "Os PINs não coincidem."
                        }
                        else -> onSave(draft)
                    }
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
