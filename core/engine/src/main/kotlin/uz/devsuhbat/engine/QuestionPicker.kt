package uz.devsuhbat.engine

import kotlin.math.roundToInt
import kotlin.random.Random
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.topicId

/** [count] questions come back for review in [inDays] days. */
data class NextReview(val inDays: Int, val count: Int)

object QuestionPicker {
    const val PRACTICE_SIZE = 10
    const val MISTAKES_SIZE = 20
    const val MOCK_SIZE = 25

    /** A mock with fewer questions than this is not offered. */
    const val MOCK_MIN = 5

    private const val MOCK_TARGET_SHARE = 0.6

    /** Share of mock questions taken from the field's own topics rather than the common ones. */
    const val MOCK_FIELD_SHARE = 0.6

    /** Questions of [maxLevel] and below, in pool order. */
    fun eligible(pool: List<Question>, maxLevel: Level): List<Question> = pool.filter { it.level <= maxLevel }

    /**
     * Up to [count] distinct eligible questions: due ones first, then unseen ones, then the rest by ascending box.
     * The order inside each group is random.
     */
    fun practice(
        pool: List<Question>,
        maxLevel: Level,
        random: Random,
        count: Int = PRACTICE_SIZE,
        states: Map<String, QuestionState> = emptyMap(),
        today: Long = 0,
    ): List<Question> {
        fun group(question: Question): Int {
            val state = states[question.id] ?: return 1
            return if (state.isDue(today)) 0 else 2
        }
        return eligible(pool, maxLevel)
            .shuffled(random)
            .sortedWith(compareBy<Question> { group(it) }.thenBy { states[it.id]?.box ?: 0 })
            .take(count)
    }

    /** Eligible questions whose due day has come, in pool order. */
    fun due(pool: List<Question>, maxLevel: Level, states: Map<String, QuestionState>, today: Long): List<Question> =
        eligible(pool, maxLevel).filter { states[it.id]?.isDue(today) == true }

    /** The earliest day after [today] on which eligible answered questions come back, or null when none is scheduled. */
    fun nextReview(pool: List<Question>, maxLevel: Level, states: Map<String, QuestionState>, today: Long): NextReview? {
        val days = eligible(pool, maxLevel).mapNotNull { states[it.id]?.dueDay }.filter { it > today }
        val first = days.minOrNull() ?: return null
        return NextReview(inDays = (first - today).toInt(), count = days.count { it == first })
    }

    /** Up to [count] due questions, most overdue first; questions due on the same day come in random order. */
    fun mistakes(
        pool: List<Question>,
        maxLevel: Level,
        states: Map<String, QuestionState>,
        today: Long,
        random: Random,
        count: Int = MISTAKES_SIZE,
    ): List<Question> =
        due(pool, maxLevel, states, today)
            .shuffled(random)
            .sortedBy { states.getValue(it.id).dueDay }
            .take(count)

    /**
     * Up to [count] questions for a mock interview: about 60% from the field's own topics and the rest from the
     * common topics ([isCommon]); inside each part about 60% are of exactly [maxLevel] and the rest of lower levels.
     * When one side has too few questions the other fills up.
     */
    fun mock(
        pool: List<Question>,
        maxLevel: Level,
        random: Random,
        count: Int = MOCK_SIZE,
        isCommon: (Question) -> Boolean = { it.topicId.startsWith("core.") },
    ): List<Question> {
        val (common, field) = eligible(pool, maxLevel).partition(isCommon)
        val (fromField, fromCommon) = split(count, MOCK_FIELD_SHARE, field.size, common.size)
        return (byLevel(field, maxLevel, fromField, random) + byLevel(common, maxLevel, fromCommon, random)).shuffled(random)
    }

    /** [n] questions of [questions]: about 60% of exactly [maxLevel], the rest of lower levels. */
    private fun byLevel(questions: List<Question>, maxLevel: Level, n: Int, random: Random): List<Question> {
        val (target, lower) = questions.shuffled(random).partition { it.level == maxLevel }
        val (fromTarget, fromLower) = split(n, MOCK_TARGET_SHARE, target.size, lower.size)
        return target.take(fromTarget) + lower.take(fromLower)
    }

    /** How many to take from a preferred side ([share] of [count]) and the other side, each filling the other's gap. */
    private fun split(count: Int, share: Double, preferred: Int, other: Int): Pair<Int, Int> {
        val fromPreferred = minOf(preferred, maxOf((count * share).roundToInt(), count - other))
        return fromPreferred to minOf(other, count - fromPreferred)
    }
}
