package uz.devsuhbat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.SettingsRepository

data class HomeUiState(val loading: Boolean = true, val fieldTitle: String? = null, val level: Level? = null)

class HomeViewModel(
    content: ContentStore,
    settings: SettingsRepository,
    io: CoroutineDispatcher,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = settings.settings
        .map { current ->
            val title = current.fieldId?.let { id -> withContext(io) { content.field(id)?.title } }
            HomeUiState(loading = false, fieldTitle = title, level = current.level)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
