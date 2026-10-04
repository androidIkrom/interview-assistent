package uz.devsuhbat

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.devsuhbat.reminder.ReminderScheduler

class DevSuhbatApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        rescheduleReminder()
    }

    /** Restores the reminder if it was lost (e.g. after the app was force-stopped); nothing to do when it is off. */
    private fun rescheduleReminder() {
        appScope.launch {
            val settings = container.settings.settings.first()
            if (settings.reminderEnabled) {
                ReminderScheduler.ensureScheduled(this@DevSuhbatApp, settings.reminderMinutes)
            }
        }
    }
}
