package uz.devsuhbat.ui.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.MockResult
import uz.devsuhbat.engine.MockSession

/** The questions of one mock and the titles of their topics, keyed by topic id. */
data class MockSetup(val questions: List<Question>, val topicTitles: Map<String, String>)

data class MockUiState(
    val loading: Boolean = true,
    /** The current question with its options in presentation order; null while loading. */
    val question: Question? = null,
    val index: Int = 0,
    val size: Int = 0,
    val selected: Set<String> = emptySet(),
    /** Indices of the questions that have an answer. */
    val answered: Set<Int> = emptySet(),
    val remainingSeconds: Int = 0,
    /** Non-null once the mock is finished. */
    val result: MockResult? = null,
    val topicTitles: Map<String, String> = emptyMap(),
)

/**
 * A mock interview with a countdown. The remaining time is derived from a deadline on the [now] clock,
 * so it stays right when ticks are delayed while the app is in the background.
 * [onFinished] is called once, when a non-empty mock ends.
 */
class MockViewModel(
    private val load: suspend () -> MockSetup,
    private val random: Random,
    private val now: () -> Long,
    private val onFinished: suspend (MockResult) -> Unit = {},
    private val durationSeconds: Int = DURATION_SECONDS,
) : ViewModel() {

    private val _state = MutableStateFlow(MockUiState())
    val state: StateFlow<MockUiState> = _state.asStateFlow()

    private var session: MockSession? = null
    private var index = 0
    private var deadline = 0L

    init {
        viewModelScope.launch {
            val setup = load()
            val loaded = MockSession(setup.questions, random)
            session = loaded
            deadline = now() + durationSeconds * MILLIS_PER_SECOND
            _state.value = MockUiState(loading = false, size = loaded.size, topicTitles = setup.topicTitles)
            if (loaded.size == 0) {
                _state.update { it.copy(result = loaded.finish()) }
            } else {
                publish()
                runTimer()
            }
        }
    }

    /** Single-choice: picks the option, or clears it when it was already picked. Multi-choice: toggles it. */
    fun toggle(optionId: String) {
        val session = activeSession() ?: return
        val question = session.questions[index]
        if (question.options.none { it.id == optionId }) return

        val current = session.selection(index)
        val selected = when {
            optionId in current -> current - optionId
            question.type == QuestionType.SINGLE -> setOf(optionId)
            else -> current + optionId
        }
        session.answer(index, selected)
        publish()
    }

    fun goTo(target: Int) {
        val session = activeSession() ?: return
        if (target !in 0 until session.size) return
        index = target
        publish()
    }

    fun next() = goTo(index + 1)

    fun previous() = goTo(index - 1)

    fun finish() {
        val session = activeSession() ?: return
        val result = session.finish()
        _state.update { it.copy(result = result, remainingSeconds = remainingSeconds()) }
        viewModelScope.launch { withContext(NonCancellable) { onFinished(result) } }
    }

    /** The session while the mock is still running; null before loading and after finishing. */
    private fun activeSession(): MockSession? = session.takeIf { _state.value.result == null }

    private suspend fun runTimer() {
        while (_state.value.result == null) {
            val remaining = remainingSeconds()
            _state.update { it.copy(remainingSeconds = remaining) }
            if (remaining == 0) {
                finish()
                return
            }
            delay(MILLIS_PER_SECOND)
        }
    }

    private fun remainingSeconds(): Int {
        val millis = (deadline - now()).coerceAtLeast(0)
        return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    private fun publish() {
        val session = session ?: return
        _state.update { current ->
            current.copy(
                question = session.questions[index],
                index = index,
                selected = session.selection(index),
                answered = (0 until session.size).filter { session.selection(it).isNotEmpty() }.toSet(),
                remainingSeconds = remainingSeconds(),
            )
        }
    }

    companion object {
        const val DURATION_SECONDS = 30 * 60
        private const val MILLIS_PER_SECOND = 1000L
    }
}
