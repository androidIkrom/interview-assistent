package uz.devsuhbat

import android.content.Context
import androidx.room.Room
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.data.AppDatabase
import uz.devsuhbat.data.ProgressRepository
import uz.devsuhbat.data.SettingsRepository
import uz.devsuhbat.data.settingsDataStore

/** Manual dependency container, created once by [DevSuhbatApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val io: CoroutineDispatcher = Dispatchers.IO

    /** Reads content from assets. Its calls touch the disk, so run them on [io]. */
    val content = ContentStore { path ->
        try {
            appContext.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        }
    }

    val settings = SettingsRepository(appContext.settingsDataStore)

    val progress = ProgressRepository(
        Room.databaseBuilder(appContext, AppDatabase::class.java, "devsuhbat.db").build()
    )
}
