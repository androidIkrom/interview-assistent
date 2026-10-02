package uz.devsuhbat.engine

import kotlin.random.Random
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Question

object QuestionPicker {
    const val PRACTICE_SIZE = 10
    const val MISTAKES_SIZE = 20

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
}
