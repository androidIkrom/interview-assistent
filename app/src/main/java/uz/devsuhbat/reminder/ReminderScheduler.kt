package uz.devsuhbat.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import uz.devsuhbat.engine.ReminderSchedule

/** Keeps exactly one pending daily reminder, at the user's chosen time of day. */
object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder"

    /** Schedules the next reminder at [minutesOfDay] (minutes after local midnight), or cancels it. */
    fun apply(context: Context, enabled: Boolean, minutesOfDay: Int) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request(minutesOfDay))
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    /**
     * Called on app start: schedules the reminder only when none is pending. KEEP, not REPLACE,
     * because WorkManager may have started the process precisely to run the pending reminder.
     */
    fun ensureScheduled(context: Context, minutesOfDay: Int) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request(minutesOfDay))
    }

    /**
     * Called by the running [ReminderWorker]: REPLACE would cancel the worker itself, so the next
     * reminder is appended and starts its delay once the current one finishes.
     */
    internal fun scheduleNextFromWorker(context: Context, minutesOfDay: Int) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request(minutesOfDay))
    }

    private fun request(minutesOfDay: Int) = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(
            ReminderSchedule.delayUntilNext(LocalDateTime.now(), minutesOfDay).toMillis(),
            TimeUnit.MILLISECONDS,
        )
        .build()
}
