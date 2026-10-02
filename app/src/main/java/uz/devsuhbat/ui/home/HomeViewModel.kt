package uz.devsuhbat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.MockSummary
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
    /** How many questions a mock interview would have now; below [QuestionPicker.MOCK_MIN] it is not offered. */
    val mockQuestionCount: Int = 0,
    val lastMock: MockSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    content: ContentStore,
    settings: Flow<UserSettings>,
    states: Flow<Map<String, QuestionState>>,
    today: () -> Long,
    io: CoroutineDispatcher,
    lastMock: (fieldId: String) -> Flow<MockSummary?> = { flowOf(null) },
) : ViewModel() {

    private val lastMockOfField: Flow<Pair<String?, MockSummary?>> = settings
        .map { it.fieldId }
        .distinctUntilChanged()
        .flatMapLatest { fieldId ->
            if (fieldId == null) flowOf(null to null) else lastMock(fieldId).map { fieldId to it }
        }

    val state: StateFlow<HomeUiState> = combine(settings, states, lastMockOfField) { current, stored, (mockField, mock) ->
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
                mockQuestionCount = minOf(scope.size, QuestionPicker.MOCK_SIZE),
                // While the field is switching, the mock flow may still carry the previous field's result.
                lastMock = mock.takeIf { mockField == current.fieldId },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
