package com.vitor.melembre.localweb

import android.content.Context

/**
 * Guarda se o acesso pelo computador deve ficar ligado. Ligado pelo usuário = o app religa o
 * servidor sozinho depois de reiniciar o celular, de atualizar o app ou se o Android derrubar o
 * serviço. Só o usuário desligando (ícone do app ou botão "Desativar" da notificação) apaga isso.
 */
object LocalAccessAuto {
    private const val KEY = "enabled"

    fun isEnabled(context: Context): Boolean = isEnabled(SharedAutoPreferences(context))

    fun setEnabled(context: Context, enabled: Boolean) = setEnabled(SharedAutoPreferences(context), enabled)

    internal fun isEnabled(preferences: PinPreferences): Boolean = preferences.getString(KEY) == "1"

    internal fun setEnabled(preferences: PinPreferences, enabled: Boolean) {
        preferences.putString(KEY, if (enabled) "1" else "0")
    }
}

private class SharedAutoPreferences(context: Context) : PinPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(
        "local_access_auto",
        Context.MODE_PRIVATE,
    )

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
}
