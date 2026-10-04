package uz.devsuhbat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupSettingsTest {

    private val settings = BackupSettings(
        fieldId = "android",
        level = "junior",
        onboardingDone = true,
        theme = "SYSTEM",
        reminderEnabled = true,
        reminderMinutes = 8 * 60,
    )

    @Test
    fun reminderIsTurnedOffWhenNotificationsAreNotAllowed() {
        val restored = settings.forDevice(notificationsAllowed = false)

        assertFalse(restored.reminderEnabled)
        assertEquals(8 * 60, restored.reminderMinutes)
    }

    @Test
    fun settingsStayAsIsWhenNotificationsAreAllowed() {
        assertEquals(settings, settings.forDevice(notificationsAllowed = true))
    }
}
