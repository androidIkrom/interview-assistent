package uz.devsuhbat.reminder

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import uz.devsuhbat.DevSuhbatApp
import uz.devsuhbat.MainActivity
import uz.devsuhbat.R
import uz.devsuhbat.engine.ReminderSchedule

/** Posts the daily reminder unless the user already practiced today, then schedules tomorrow's. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as DevSuhbatApp).container
        val settings = container.settings.settings.first()
        if (!settings.reminderEnabled) return Result.success()

        val practiced = ReminderSchedule.practicedToday(
            container.progress.lastFinishedAt(),
            Instant.now(),
            ZoneId.systemDefault(),
        )
        if (!practiced) notify(applicationContext)

        ReminderScheduler.scheduleNextFromWorker(applicationContext, settings.reminderMinutes)
        return Result.success()
    }

    private fun notify(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 1
    }
}
