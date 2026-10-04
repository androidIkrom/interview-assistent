package uz.devsuhbat.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val dataStoreScope = CoroutineScope(dispatcher + Job())

    private val repository by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(tmp.root, "settings.preferences_pb") }
        )
    }

    @After
    fun tearDown() = dataStoreScope.cancel()

    @Test
    fun reminderIsOffAtEightPmByDefault() = runTest(dispatcher) {
        val settings = repository.settings.first()

        assertFalse(settings.reminderEnabled)
        assertEquals(20 * 60, settings.reminderMinutes)
    }

    @Test
    fun setReminderIsReadBack() = runTest(dispatcher) {
        repository.setReminder(enabled = true, minutes = 8 * 60)

        val settings = repository.settings.first()
        assertTrue(settings.reminderEnabled)
        assertEquals(8 * 60, settings.reminderMinutes)
    }
}
