package com.vitor.melembre.focus

import android.content.Context

/** Guarda e lê a agenda do modo foco (usada pelo app e pela página do computador). */
interface FocusAgenda {
    fun load(): List<FocusBlock>
    fun save(blocks: List<FocusBlock>)
}

/** Verdadeiro enquanto a tela de foco está aberta; os lembretes ficam em silêncio nesse período. */
object FocusMode {
    @Volatile
    var active: Boolean = false
}

class FocusStore(context: Context) : FocusAgenda {
    private val prefs = context.applicationContext.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun load(): List<FocusBlock> {
        val stored = prefs.getString(KEY_AGENDA, null) ?: return FocusSchedule.DEFAULT
        return FocusSchedule.decode(stored) ?: FocusSchedule.DEFAULT
    }

    override fun save(blocks: List<FocusBlock>) {
        prefs.edit().putString(KEY_AGENDA, FocusSchedule.encode(blocks)).commit()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_AGENDA = "agenda"
    }
}
