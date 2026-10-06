package com.vitor.melembre.localweb

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocalAccessSnapshot(
    val active: Boolean = false,
    val starting: Boolean = false,
    val url: String? = null,
    val pin: String? = null,
    val detail: String? = null,
)

object LocalAccessController {
    const val PORT = 8765
    const val NO_WIFI_MESSAGE =
        "Conecte o celular a uma rede Wi-Fi para usar o acesso pelo computador."
    const val SAME_WIFI_MESSAGE =
        "O computador deve estar conectado à mesma rede Wi-Fi."

    private val _state = MutableStateFlow(LocalAccessSnapshot())
    val state: StateFlow<LocalAccessSnapshot> = _state.asStateFlow()

    fun markStarting() {
        _state.value = LocalAccessSnapshot(starting = true)
    }

    fun markOn(url: String, pin: String) {
        _state.value = LocalAccessSnapshot(active = true, url = url, pin = pin)
    }

    fun markOff() {
        _state.value = LocalAccessSnapshot()
    }

    fun markUnavailable() {
        _state.value = LocalAccessSnapshot(detail = NO_WIFI_MESSAGE)
    }

    fun markError(message: String) {
        _state.value = LocalAccessSnapshot(detail = message)
    }
}
