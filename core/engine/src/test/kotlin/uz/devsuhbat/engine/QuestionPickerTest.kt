package uz.devsuhbat.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.devsuhbat.content.Level
import uz.devsuhbat.single

class QuestionPickerTest {

    private val pool =
        (1..4).map { single("j.00$it", Level.JUNIOR) } +
            (1..3).map { single("m.00$it", Level.MIDDLE) } +
            (1..2).map { single("s.00$it", Level.STRONG_MIDDLE) } +
            single("x.001", Level.SENIOR)

    @Test
    fun eligibleIncludesLowerLevels() {
        assertEquals(4, QuestionPicker.eligible(pool, Level.JUNIOR).size)
        assertEquals(7, QuestionPicker.eligible(pool, Level.MIDDLE).size)
        assertEquals(9, QuestionPicker.eligible(pool, Level.STRONG_MIDDLE).size)
        assertEquals(10, QuestionPicker.eligible(pool, Level.SENIOR).size)
    }

    @Test
    fun eligibleKeepsOrder() {
        assertEquals(pool.take(7), QuestionPicker.eligible(pool, Level.MIDDLE))
    }

    @Test
    fun practiceReturnsDistinctQuestionsUpToCount() {
        val picked = QuestionPicker.practice(pool, Level.SENIOR, Random(7), count = 5)

        assertEquals(5, picked.size)
        assertEquals(5, picked.map { it.id }.toSet().size)
        assertTrue(pool.containsAll(picked))
    }

    @Test
    fun practiceNeverPicksAboveTheLevel() {
        repeat(20) { seed ->
            val picked = QuestionPicker.practice(pool, Level.JUNIOR, Random(seed), count = 3)

            assertTrue(picked.all { it.level == Level.JUNIOR })
        }
    }

    @Test
    fun practiceReturnsAllWhenPoolIsSmall() {
        val picked = QuestionPicker.practice(pool, Level.JUNIOR, Random(7))

        assertEquals(4, picked.size)
        assertEquals(pool.take(4).map { it.id }.toSet(), picked.map { it.id }.toSet())
    }

    @Test
    fun practiceDefaultsToTenQuestions() {
        val big = (1..30).map { single("j.${it.toString().padStart(3, '0')}") }

        assertEquals(10, QuestionPicker.practice(big, Level.JUNIOR, Random(7)).size)
    }

    @Test
    fun practiceOfEmptyPoolIsEmpty() {
        assertEquals(emptyList<Any>(), QuestionPicker.practice(emptyList(), Level.SENIOR, Random(7)))
    }
}
