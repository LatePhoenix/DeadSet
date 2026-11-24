package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.history.monthDayFromIso
import com.example.data.local.AppDatabase
import com.example.data.todayIsoDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Home-screen "Today in Dead History" widget (6.5 / H3).
 * Plain AppWidgetProvider + RemoteViews (no Glance — keeps dependencies lean).
 * Catalog-only (`isCustomShow = 0`), loaded off the main thread; midnight alarm refreshes the day.
 */
class TodayInDeadHistoryWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MIDNIGHT_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    refreshAllAsync(context)
                    scheduleMidnightRefresh(context)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
                scheduleMidnightRefresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        scheduleMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(midnightPendingIntent(context))
    }

    companion object {
        const val ACTION_MIDNIGHT_REFRESH = "com.example.widget.ACTION_TODAY_IN_DEAD_HISTORY_MIDNIGHT"

        /** Fire-and-forget refresh of every installed widget (app start). */
        fun refreshAll(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                refreshAllAsync(context)
            }
        }

        private suspend fun refreshAllAsync(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TodayInDeadHistoryWidget::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        suspend fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val today = todayIsoDate()
            val monthDay = monthDayFromIso(today)
            val showLines = AppDatabase.getInstance(context).showDao()
                .getCatalogShowsOnMonthDay(monthDay)
                .map { "${it.date} · ${it.venue}" }
            val lines = widgetBodyLines(monthDay, showLines)
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

        fun scheduleMidnightRefresh(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Inexact / allow-while-idle — no SCHEDULE_EXACT_ALARM permission (H3).
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextLocalMidnightMillis(),
                midnightPendingIntent(context)
            )
        }

        private fun midnightPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, TodayInDeadHistoryWidget::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Next local 00:01 (Calendar, not java.time). */
        fun nextLocalMidnightMillis(now: Calendar = Calendar.getInstance()): Long {
            val cal = now.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 1)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
