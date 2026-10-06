package com.vitor.melembre.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.vitor.melembre.MainActivity
import com.vitor.melembre.R
import com.vitor.melembre.voice.VoiceRecognitionHelper

object VoiceShortcutHelper {
    const val SHORTCUT_ID = "falar_lembrete"

    fun publishDynamicShortcut(context: Context) {
        ShortcutManagerCompat.pushDynamicShortcut(context, buildShortcut(context))
    }

    fun requestPin(context: Context): PinResult {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return PinResult.Unsupported
        }
        publishDynamicShortcut(context)
        if (isAlreadyPinned(context)) {
            return PinResult.AlreadyPinned
        }
        val accepted = ShortcutManagerCompat.requestPinShortcut(context, buildShortcut(context), null)
        return if (accepted) PinResult.Requested else PinResult.Unsupported
    }

    private fun isAlreadyPinned(context: Context): Boolean {
        return try {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .any { it.id == SHORTCUT_ID }
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun buildShortcut(context: Context): ShortcutInfoCompat {
        val label = context.getString(R.string.voice_shortcut_label)
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_voice))
            .setIntent(voiceIntent(context))
            .build()
    }

    private fun voiceIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = VoiceRecognitionHelper.ACTION_START_VOICE_REMINDER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    enum class PinResult {
        Requested,
        AlreadyPinned,
        Unsupported,
    }
}
