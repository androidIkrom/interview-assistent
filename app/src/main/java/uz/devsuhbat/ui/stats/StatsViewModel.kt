package uz.devsuhbat.ui.stats

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
import uz.devsuhbat.engine.ActivityGrid
import uz.devsuhbat.engine.Breakdown
import uz.devsuhbat.engine.LeitnerBreakdown
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.Readiness
import uz.devsuhbat.engine.Streak
import uz.devsuhbat.engine.StreakInfo
import uz.devsuhbat.ui.home.TopicProgress

/**
 * Streak and [grid] count sessions of every field; [breakdown], [topics] and [mocks] belong to the chosen
 * field and level. [topics] runs weakest first and leaves out topics without a question at the level.
 */
data class StatsUiState(
    val loading: Boolean = true,
    val fieldTitle: String? = null,
    val level: Level? = null,
    val streak: StreakInfo = Streak.NONE,
    val grid: List<List<Int?>> = emptyList(),
    val breakdown: Breakdown = Breakdown(0, 0, 0),
    val topics: List<TopicProgress> = emptyList(),
    val mocks: List<MockSummary> = emptyList(),
    val mockAvailable: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    content: ContentStore,
    settings: Flow<UserSettings>,
    states: Flow<Map<String, QuestionState>>,
    sessionDays: Flow<List<Long>>,
    today: () -> Long,
    io: CoroutineDispatcher,
    mockHistory: (fieldId: String) -> Flow<List<MockSummary>> = { flowOf(emptyList()) },
) : ViewModel() {

    private val mocksOfField: Flow<Pair<String?, List<MockSummary>>> = settings
        .map { it.fieldId }
        .distinctUntilChanged()
        .flatMapLatest { fieldId ->
            if (fieldId == null) flowOf(null to emptyList()) else mockHistory(fieldId).map { fieldId to it }
        }

    val state: StateFlow<StatsUiState> =
        combine(settings, states, sessionDays, mocksOfField) { current, stored, days, (mockField, mocks) ->
            withContext(io) {
                val field = current.fieldId?.let(content::field)
                val level = current.level
                val topics = if (field == null || level == null) {
                    emptyList()
                } else {
                    content.topicsOf(field.id).map { topic ->
                        TopicProgress(topic.id, topic.title, Readiness.of(QuestionPicker.eligible(content.questions(topic.id), level), stored))
                    }
                }
                val scope = if (field == null || level == null) {
                    emptyList()
                } else {
                    QuestionPicker.eligible(content.topicsOf(field.id).flatMap { content.questions(it.id) }, level)
                }
                val day = today()
                StatsUiState(
                    loading = false,
                    fieldTitle = field?.title,
                    level = level,
                    streak = Streak.of(days.toSet(), day),
                    grid = ActivityGrid.of(days, day),
                    breakdown = LeitnerBreakdown.of(scope, stored),
                    topics = topics
                        .filter { it.progress.total > 0 }
                        .sortedWith(compareBy<TopicProgress> { it.progress.percent }.thenByDescending { it.progress.total }),
                    // While the field is switching, the mock flow may still carry the previous field's history.
                    mocks = if (mockField == current.fieldId) mocks else emptyList(),
                    mockAvailable = scope.size >= QuestionPicker.MOCK_MIN,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
