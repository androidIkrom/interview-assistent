package uz.devsuhbat.ui.topics

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
import uz.devsuhbat.content.Topic
import uz.devsuhbat.data.UserSettings
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.Readiness

/**
 * [questionCount] counts the questions eligible for the user's level; 0 means the topic cannot be started.
 * [progress] is the mastered share of those questions.
 */
data class TopicRow(val topic: Topic, val questionCount: Int, val progress: Progress)

data class TopicsUiState(
    val loading: Boolean = true,
    val rows: List<TopicRow> = emptyList(),
    val mixedCount: Int = 0,
)

class TopicsViewModel(
    content: ContentStore,
    settings: Flow<UserSettings>,
    states: Flow<Map<String, QuestionState>>,
    io: CoroutineDispatcher,
) : ViewModel() {

    val state: StateFlow<TopicsUiState> = combine(settings, states) { current, stored ->
        val fieldId = current.fieldId
        val level = current.level
        val rows = if (fieldId == null || level == null) emptyList() else withContext(io) {
            content.topicsOf(fieldId)
                .map { topic ->
                    val eligible = QuestionPicker.eligible(content.questions(topic.id), level)
                    TopicRow(topic, eligible.size, Readiness.of(eligible, stored))
                }
                // Stable sort: topics that can be started come first, each part keeps the field's order.
                .sortedByDescending { it.questionCount > 0 }
        }
        TopicsUiState(loading = false, rows = rows, mixedCount = rows.sumOf { it.questionCount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TopicsUiState())
}
