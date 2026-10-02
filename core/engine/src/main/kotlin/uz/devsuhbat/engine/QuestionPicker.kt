package uz.devsuhbat.engine

import kotlin.random.Random
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Question

object QuestionPicker {
    const val PRACTICE_SIZE = 10

    /** Questions of [maxLevel] and below, in pool order. */
    fun eligible(pool: List<Question>, maxLevel: Level): List<Question> = pool.filter { it.level <= maxLevel }

    /** Up to [count] distinct eligible questions in random order. */
    fun practice(pool: List<Question>, maxLevel: Level, random: Random, count: Int = PRACTICE_SIZE): List<Question> =
        eligible(pool, maxLevel).shuffled(random).take(count)
}
