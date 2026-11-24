package com.hyrulecompanion.app.updates

import com.hyrulecompanion.app.ui.translate
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.hyrulecompanion.app.MainActivity
import com.hyrulecompanion.app.R
import java.util.concurrent.TimeUnit

/** Update notification state is local and NOT part of the user's Supabase journey backup. */
class AppUpdatePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hc_app_updates", Context.MODE_PRIVATE)
    var backgroundNotices: Boolean
        get() = prefs.getBoolean("background_notices", false)
        set(value) { prefs.edit().putBoolean("background_notices", value).apply() }

    fun shouldShowDialog(versionCode: Int): Boolean =
        versionCode != prefs.getInt("dismissed_code", -1) ||
            System.currentTimeMillis() - prefs.getLong("dismissed_at", 0L) >= TimeUnit.HOURS.toMillis(24)

    fun postpone(versionCode: Int) {
        prefs.edit().putInt("dismissed_code", versionCode)
            .putLong("dismissed_at", System.currentTimeMillis()).apply()
    }

    fun wasNotified(versionCode: Int) = prefs.getInt("notified_code", -1) >= versionCode
    fun markNotified(versionCode: Int) { prefs.edit().putInt("notified_code", versionCode).apply() }
}

object AppUpdateScheduler {
    private const val UNIQUE_WORK = "hyrule_version_check"
    const val CHANNEL = "hyrule_new_versions"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Kick off the first background check as soon as the user enables notifications. */
    fun checkOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork("hyrule_version_check_now", ExistingWorkPolicy.REPLACE, request)
    }

    fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun notify(context: Context, release: AppRelease): Boolean {
        if (!notificationsAllowed(context)) return false
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL, translate("Atualizações do Hyrule Companion"), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_update_notice", true)
        }
        val pending = PendingIntent.getActivity(context, release.versionCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_hyrule)
            .setContentTitle(translate("Hyrule Companion ${release.versionName} disponível"))
            .setContentText(translate("Uma nova jornada espera por você. Toque para atualizar."))
            .setStyle(NotificationCompat.BigTextStyle().bigText(translate("Nova versão ${release.versionName} disponível. Abra o aplicativo para ver as novidades e atualizar.")))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(41007, notification)
            true
        }.getOrDefault(false)
    }
}

class AppUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val prefs = AppUpdatePreferences(applicationContext)
        if (!prefs.backgroundNotices || !AppUpdateScheduler.notificationsAllowed(applicationContext)) return Result.success()
        return try {
            val release = AppUpdateRepository().latest()
            if (release != null && release.isNewer && !prefs.wasNotified(release.versionCode)) {
                if (AppUpdateScheduler.notify(applicationContext, release)) prefs.markNotified(release.versionCode)
            }
            Result.success()
        } catch (error: Exception) {
            // A not-yet-created table should not cause endless retries.
            if (error.message?.contains("não foi ativado", ignoreCase = true) == true) Result.success()
            else Result.retry()
        }
    }
}
