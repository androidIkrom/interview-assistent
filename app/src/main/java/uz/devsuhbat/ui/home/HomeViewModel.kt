package uz.devsuhbat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.UserSettings
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.Readiness

data class HomeUiState(
    val loading: Boolean = true,
    val fieldTitle: String? = null,
    val level: Level? = null,
    /** Mastered questions among those of the chosen level and below. */
    val readiness: Progress = Progress(0, 0),
    /** Questions waiting in the "Xatolar" session today. */
    val dueCount: Int = 0,
)

class HomeViewModel(
    content: ContentStore,
    settings: Flow<UserSettings>,
    states: Flow<Map<String, QuestionState>>,
    today: () -> Long,
    io: CoroutineDispatcher,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(settings, states) { current, stored ->
        withContext(io) {
            val field = current.fieldId?.let(content::field)
            val level = current.level
            val scope = if (field == null || level == null) {
                emptyList()
            } else {
                QuestionPicker.eligible(content.topicsOf(field.id).flatMap { content.questions(it.id) }, level)
            }
            HomeUiState(
                loading = false,
                fieldTitle = field?.title,
                level = level,
                readiness = Readiness.of(scope, stored),
                dueCount = scope.count { stored[it.id]?.isDue(today()) == true },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
