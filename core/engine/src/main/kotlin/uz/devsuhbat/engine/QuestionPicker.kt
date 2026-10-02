package uz.devsuhbat.engine

import kotlin.math.roundToInt
import kotlin.random.Random
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Question

object QuestionPicker {
    const val PRACTICE_SIZE = 10
    const val MISTAKES_SIZE = 20
    const val MOCK_SIZE = 25

    /** A mock with fewer questions than this is not offered. */
    const val MOCK_MIN = 5

    private const val MOCK_TARGET_SHARE = 0.6

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
     * Up to [count] questions for a mock interview: about 60% of exactly [maxLevel], the rest from lower levels.
     * When one side has too few questions the other fills up.
     */
    fun mock(pool: List<Question>, maxLevel: Level, random: Random, count: Int = MOCK_SIZE): List<Question> {
        val (target, lower) = eligible(pool, maxLevel).shuffled(random).partition { it.level == maxLevel }
        val targetWanted = (count * MOCK_TARGET_SHARE).roundToInt()
        val fromTarget = minOf(target.size, maxOf(targetWanted, count - lower.size))
        val fromLower = minOf(lower.size, count - fromTarget)
        return (target.take(fromTarget) + lower.take(fromLower)).shuffled(random)
    }
}
