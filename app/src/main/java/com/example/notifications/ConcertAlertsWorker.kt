package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.alerts.ConcertAlertRules
import com.example.data.local.AppDatabase
import com.example.data.local.UserPrefsStore
import com.example.data.repository.ConcertRepository
import com.example.data.todayIsoDate
import java.util.concurrent.TimeUnit

/**
 * Background check (step 4.6), about every 12 hours while alerts are on: refreshes followed
 * bands' dates, notifies about dates that weren't known before, and once per day reminds about
 * catalog shows happening today.
 */
class ConcertAlertsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPrefsStore(applicationContext)
        if (!prefs.alertsEnabled()) return Result.success()
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.concertDao()
        val today = todayIsoDate()

        val knownBefore = dao.eventIds().toSet()
        val firstSync = !prefs.hasSyncedEvents()
        val outcome = runCatching { ConcertRepository(dao).refresh() }.getOrNull()
        if (outcome?.anySourceReached == true) prefs.markEventsSynced()
        val after = dao.upcomingEventsOnce(today).map { it.toEvent() }
        val fresh = ConcertAlertRules.newEvents(knownBefore, after, firstSync)
        if (fresh.isNotEmpty()) {
            notify(NEW_DATES_ID, ConcertAlertRules.newDatesTitle(fresh), ConcertAlertRules.newDatesBody(fresh))
        }

        val tonight = ConcertAlertRules.tonightShows(db.showDao().getAllShowsOnce(), today, prefs.lastTonightReminder())
        if (tonight.isNotEmpty()) {
            if (notify(TONIGHT_ID, ConcertAlertRules.tonightTitle(tonight), ConcertAlertRules.tonightBody(tonight))) {
                prefs.setLastTonightReminder(today)
            }
        }
        return Result.success()
    }

    /** Posts a notification; returns false when skipped (e.g. missing POST_NOTIFICATIONS on API 33+). */
    private fun notify(id: Int, title: String, body: String): Boolean {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "concert_alerts"
        private const val WORK_NAME = "concert-alerts"
        private const val NEW_DATES_ID = 4601
        private const val TONIGHT_ID = 4602

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Show alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "New dates from bands you follow, and day-of show reminders"
                    }
                )
            }
        }

        /** Starts (or keeps) the periodic check; safe to call repeatedly. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ConcertAlertsWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
