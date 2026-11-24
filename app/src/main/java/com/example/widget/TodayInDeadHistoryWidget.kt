package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.history.monthDayFromIso
import com.example.data.history.showsOnTodayInHistory
import com.example.data.local.AppDatabase
import com.example.data.todayIsoDate
import kotlinx.coroutines.runBlocking

/**
 * Home-screen "Today in Dead History" widget (6.5).
 * Plain AppWidgetProvider + RemoteViews (no Glance — keeps dependencies lean).
 */
class TodayInDeadHistoryWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val today = todayIsoDate()
            val monthDay = monthDayFromIso(today)
            val lines = runBlocking {
                val shows = AppDatabase.getInstance(context).showDao().getAllShowsOnce()
                val matches = showsOnTodayInHistory(shows, today)
                widgetBodyLines(monthDay, matches.map { "${it.date} · ${it.venue}" })
            }
            val views = RemoteViews(context.packageName, R.layout.widget_today_in_dead_history).apply {
                setTextViewText(R.id.widget_title, "Today in Dead History")
                setTextViewText(R.id.widget_body, lines.joinToString("\n"))
                val open = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pending = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pending)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /** Pure lines for tests: empty catalog day says so. */
        fun widgetBodyLines(monthDay: String, showLines: List<String>): List<String> {
            if (showLines.isEmpty()) {
                return listOf("No shows on $monthDay in the catalog.")
            }
            return listOf("$monthDay") + showLines.take(4) +
                if (showLines.size > 4) listOf("… +${showLines.size - 4} more") else emptyList()
        }
    }
}
