package com.vitor.melembre.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vitor.melembre.MainActivity
import com.vitor.melembre.R
import com.vitor.melembre.voice.VoiceRecognitionHelper

class VoiceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.voice_widget)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = VoiceRecognitionHelper.ACTION_START_VOICE_REMINDER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.voice_widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.voice_widget_icon, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
