package uz.devsuhbat.engine

import kotlin.random.Random
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionKind

/** How the first presentation of a question went. Repeats within the session add no outcome. */
data class QuestionOutcome(val questionId: String, val firstTryCorrect: Boolean, val wrongSubmissions: Int)

data class SessionResult(val total: Int, val firstTryCorrect: Int, val reworked: Int)

/**
 * A queue of questions answered one at a time. A question that took at least one wrong submission is
 * presented once more at the end of the queue, with its options in a new order.
 */
class PracticeSession(questions: List<Question>, private val random: Random) {
    private class Item(val question: Question, val isRepeat: Boolean)

    private val queue = questions.map { Item(it, isRepeat = false) }.toMutableList()
    private var index = 0
    private var current: QuestionAttempt? = null
    private val recorded = mutableListOf<QuestionOutcome>()

    /** Number of distinct questions. */
    val total: Int = questions.size

    /** [total] plus the repeats appended so far. */
    val queueSize: Int get() = queue.size

    /** 0-based index of the current item in the queue. */
    val position: Int get() = index

    /** The current item with its options in presentation order; null when the session is finished. */
    val attempt: QuestionAttempt? get() = current

    val isRepeat: Boolean get() = queue.getOrNull(index)?.isRepeat ?: false

    val finished: Boolean get() = index >= queue.size

    val outcomes: List<QuestionOutcome> get() = recorded.toList()

    init {
        present()
    }

    fun submit(selected: Set<String>): Verdict {
        val before = checkNotNull(current) { "the session is finished" }
        val (after, verdict) = before.submit(selected)
        current = after
        if (after.solved && !isRepeat) {
            recorded += QuestionOutcome(after.question.id, after.firstTryCorrect, after.wrongSubmissions)
        }
        return verdict
    }

    fun next() {
        val attempt = checkNotNull(current) { "the session is finished" }
        check(attempt.solved) { "the current question is not solved yet" }
        val item = queue[index]
        if (!item.isRepeat && attempt.wrongSubmissions > 0) {
            queue += Item(item.question, isRepeat = true)
        }
        index++
        present()
    }

    fun result(): SessionResult {
        check(finished) { "the session is not finished" }
        val firstTry = recorded.count { it.firstTryCorrect }
        return SessionResult(total = total, firstTryCorrect = firstTry, reworked = total - firstTry)
    }

    private fun present() {
        current = queue.getOrNull(index)?.let { QuestionAttempt(shuffled(it.question)) }
    }

    /** True/false keeps its file order so "Ha" always comes before "Yo'q". */
    private fun shuffled(question: Question): Question =
        if (question.kind == QuestionKind.TRUE_FALSE) question
        else question.copy(options = question.options.shuffled(random))
}
