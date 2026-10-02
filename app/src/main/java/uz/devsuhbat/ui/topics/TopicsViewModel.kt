package uz.devsuhbat.ui.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Topic
import uz.devsuhbat.data.SettingsRepository
import uz.devsuhbat.engine.QuestionPicker

/** [questionCount] counts the questions eligible for the user's level; 0 means the topic cannot be started. */
data class TopicRow(val topic: Topic, val questionCount: Int)

data class TopicsUiState(
    val loading: Boolean = true,
    val rows: List<TopicRow> = emptyList(),
    val mixedCount: Int = 0,
)

class TopicsViewModel(
    private val content: ContentStore,
    private val settings: SettingsRepository,
    private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(TopicsUiState())
    val state: StateFlow<TopicsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.settings.first()
            val fieldId = current.fieldId
            val level = current.level
            val rows = if (fieldId == null || level == null) emptyList() else withContext(io) {
                content.topicsOf(fieldId)
                    .map { TopicRow(it, QuestionPicker.eligible(content.questions(it.id), level).size) }
                    // Stable sort: topics that can be started come first, each part keeps the field's order.
                    .sortedByDescending { it.questionCount > 0 }
            }
            _state.value = TopicsUiState(loading = false, rows = rows, mixedCount = rows.sumOf { it.questionCount })
        }
    }
}
