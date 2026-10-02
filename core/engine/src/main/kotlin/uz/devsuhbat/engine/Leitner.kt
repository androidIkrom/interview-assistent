package uz.devsuhbat.engine

/** What the app remembers about one question. Days are epoch days of the local calendar. */
data class QuestionState(
    val questionId: String,
    val box: Int,
    val dueDay: Long,
    val attempts: Int = 0,
    val wrongAttempts: Int = 0,
) {
    val mastered: Boolean get() = box >= Leitner.MASTERED_BOX

    fun isDue(today: Long): Boolean = dueDay <= today
}

/** Spaced repetition over five boxes: a higher box means a longer wait until the question returns. */
object Leitner {
    const val FIRST_BOX = 1
    const val MASTERED_BOX = 3
    const val MAX_BOX = 5

    /** A question answered correctly the first time it is ever seen skips the short intervals. */
    private const val NEW_CORRECT_BOX = 3

    private val intervals = longArrayOf(1, 3, 7, 14, 30)

    fun intervalDays(box: Int): Long {
        require(box in FIRST_BOX..MAX_BOX) { "box must be in $FIRST_BOX..$MAX_BOX, was $box" }
        return intervals[box - 1]
    }

    fun afterOutcome(previous: QuestionState?, outcome: QuestionOutcome, today: Long): QuestionState {
        val box = when {
            !outcome.firstTryCorrect -> FIRST_BOX
            previous == null -> NEW_CORRECT_BOX
            else -> minOf(previous.box + 1, MAX_BOX)
        }
        return QuestionState(
            questionId = outcome.questionId,
            box = box,
            dueDay = today + intervalDays(box),
            attempts = (previous?.attempts ?: 0) + 1,
            wrongAttempts = (previous?.wrongAttempts ?: 0) + outcome.wrongSubmissions,
        )
    }

    /**
     * A mock interview gives no feedback, so a missed question has not been worked through yet:
     * it is due today and shows up in the mistakes session at once. A correct answer counts as a first try.
     */
    fun afterMock(previous: QuestionState?, questionId: String, correct: Boolean, today: Long): QuestionState {
        if (correct) {
            return afterOutcome(previous, QuestionOutcome(questionId, firstTryCorrect = true, wrongSubmissions = 0), today)
        }
        return QuestionState(
            questionId = questionId,
            box = FIRST_BOX,
            dueDay = today,
            attempts = (previous?.attempts ?: 0) + 1,
            wrongAttempts = (previous?.wrongAttempts ?: 0) + 1,
        )
    }
}
