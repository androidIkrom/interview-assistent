package uz.devsuhbat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import uz.devsuhbat.content.Level

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val fieldId: String?,
    val level: Level?,
    val onboardingDone: Boolean,
    val theme: ThemeMode,
)

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            UserSettings(
                fieldId = prefs[FIELD],
                level = Level.entries.firstOrNull { it.storageId == prefs[LEVEL] },
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
                theme = ThemeMode.entries.firstOrNull { it.name == prefs[THEME] } ?: ThemeMode.SYSTEM,
            )
        }

    suspend fun completeOnboarding(fieldId: String, level: Level) {
        dataStore.edit { prefs ->
            prefs[FIELD] = fieldId
            prefs[LEVEL] = level.storageId
            prefs[ONBOARDING_DONE] = true
        }
    }

    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[THEME] = theme.name }
    }

    private companion object {
        val FIELD = stringPreferencesKey("field")
        val LEVEL = stringPreferencesKey("level")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME = stringPreferencesKey("theme")

        /** Same ids as the content JSON: junior, middle, strong_middle, senior. */
        val Level.storageId: String get() = name.lowercase()
    }
}
