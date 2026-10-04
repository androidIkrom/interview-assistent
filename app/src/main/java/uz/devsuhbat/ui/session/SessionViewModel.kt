package uz.devsuhbat.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.PracticeSession
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionOutcome
import uz.devsuhbat.engine.SessionResult
import uz.devsuhbat.engine.Verdict

sealed interface Feedback {
    /**
     * [hints] pairs the text of each wrong option just picked with its hint.
     * [correctPicked] and [correctMissing] are set only for multi-choice questions, and are counts:
     * wrong feedback never says which options are correct.
     */
    data class Wrong(val hints: List<Pair<String, String>>, val correctPicked: Int?, val correctMissing: Int?) : Feedback

    data class Correct(val explanation: String) : Feedback
}

data class SessionUiState(
    val loading: Boolean = true,
    /** The current question with its options in presentation order; null while loading and when finished. */
    val question: Question? = null,
    val isRepeat: Boolean = false,
    val position: Int = 0,
    val queueSize: Int = 0,
    val selected: Set<String> = emptySet(),
    val disabled: Set<String> = emptySet(),
    val feedback: Feedback? = null,
    val solved: Boolean = false,
    val canCheck: Boolean = false,
    /** Non-null once the session is finished. */
    val result: SessionResult? = null,
    /** Readiness of the field and level when the session started; null without a chosen field. */
    val readinessBefore: Progress? = null,
    /** Readiness once every answer and the session itself are saved; null until then. */
    val readinessAfter: Progress? = null,
)

/**
 * [onOutcome] is called once per distinct question, as soon as its first presentation is solved, so answers
 * survive the app being killed mid-session. [onFinished] is called once, when a non-empty session ends.
 */
class SessionViewModel(
    private val loadQuestions: suspend () -> List<Question>,
    private val random: Random,
    private val onOutcome: suspend (QuestionOutcome) -> Unit = {},
    private val onFinished: suspend (SessionResult) -> Unit = {},
    private val readiness: suspend () -> Progress? = { null },
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private var session: PracticeSession? = null
    private var reportedOutcomes = 0
    private var readinessBefore: Progress? = null
    private var readinessAfter: Progress? = null

    /** Answer writes still running; the readiness after the session must wait for all of them. */
    private val writes = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            readinessBefore = readiness()
            session = PracticeSession(loadQuestions(), random)
            publish(selected = emptySet(), feedback = null)
        }
    }

    /** Single-choice: replaces the selection. Multi-choice: toggles the option. */
    fun toggle(optionId: String) {
        val attempt = session?.attempt ?: return
        if (attempt.solved || optionId in attempt.disabled) return
        if (attempt.question.options.none { it.id == optionId }) return

        val current = _state.value.selected
        val selected = when (attempt.question.type) {
            QuestionType.SINGLE -> setOf(optionId)
            QuestionType.MULTI -> if (optionId in current) current - optionId else current + optionId
        }
        publish(selected, _state.value.feedback)
    }

    fun check() {
        val session = session ?: return
        val current = _state.value
        val question = current.question ?: return
        if (!current.canCheck) return

        when (val verdict = session.submit(current.selected)) {
            Verdict.Correct -> {
                reportNewOutcomes(session)
                publish(current.selected, Feedback.Correct(question.explanation))
            }
            is Verdict.Wrong -> {
                val multi = question.type == QuestionType.MULTI
                val feedback = Feedback.Wrong(
                    hints = verdict.wrongPicked.map { it.text to it.hint.orEmpty() },
                    correctPicked = verdict.correctPicked.takeIf { multi },
                    correctMissing = verdict.correctMissing.takeIf { multi },
                )
                val eliminated = verdict.wrongPicked.map { it.id }.toSet()
                publish(current.selected - eliminated, feedback)
            }
        }
    }

    fun next() {
        val session = session ?: return
        if (!_state.value.solved) return
        session.next()
        publish(selected = emptySet(), feedback = null)
        if (session.finished && session.total > 0) {
            val result = session.result()
            val pending = writes.toList()
            viewModelScope.launch {
                withContext(NonCancellable) {
                    pending.joinAll()
                    onFinished(result)
                }
                readinessAfter = readiness()
                publish(selected = emptySet(), feedback = null)
            }
        }
    }

    private fun reportNewOutcomes(session: PracticeSession) {
        val fresh = session.outcomes.drop(reportedOutcomes)
        reportedOutcomes += fresh.size
        fresh.forEach { outcome -> persist { onOutcome(outcome) } }
    }

    /** Leaving the screen right after answering must not cancel a write that has started. */
    private fun persist(block: suspend () -> Unit) {
        writes += viewModelScope.launch { withContext(NonCancellable) { block() } }
    }

    private fun publish(selected: Set<String>, feedback: Feedback?) {
        val session = session ?: return
        val attempt = session.attempt
        _state.value = if (attempt == null) {
            SessionUiState(
                loading = false,
                position = session.position,
                queueSize = session.queueSize,
                result = session.result(),
                readinessBefore = readinessBefore,
                readinessAfter = readinessAfter,
            )
        } else {
            SessionUiState(
                loading = false,
                question = attempt.question,
                isRepeat = session.isRepeat,
                position = session.position,
                queueSize = session.queueSize,
                selected = selected,
                disabled = attempt.disabled,
                feedback = feedback,
                solved = attempt.solved,
                canCheck = attempt.canSubmit(selected),
                readinessBefore = readinessBefore,
            )
        }
    }
}
