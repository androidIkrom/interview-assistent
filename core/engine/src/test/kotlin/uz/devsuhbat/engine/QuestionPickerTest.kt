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

    // --- state-aware picking; today = 100 ---

    private val today = 100L
    private val six = (1..6).map { single("q.00$it") }

    private fun state(id: String, box: Int, dueDay: Long) = id to QuestionState(id, box = box, dueDay = dueDay)

    @Test
    fun practicePrefersDueThenUnseenThenLowestBox() {
        val states = mapOf(state("q.001", 1, 99), state("q.002", 4, 200), state("q.003", 2, 150))

        repeat(10) { seed ->
            val picked = QuestionPicker.practice(six, Level.JUNIOR, Random(seed), count = 5, states = states, today = today)
                .map { it.id }

            assertEquals("q.001", picked.first())
            assertEquals(setOf("q.004", "q.005", "q.006"), picked.subList(1, 4).toSet())
            assertEquals("q.003", picked.last())
        }
    }

    @Test
    fun dueReturnsOnlyDueQuestionsInPoolOrder() {
        val states = mapOf(state("q.003", 1, 100), state("q.001", 2, 90), state("q.002", 3, 101))

        assertEquals(listOf("q.001", "q.003"), QuestionPicker.due(six, Level.JUNIOR, states, today).map { it.id })
    }

    @Test
    fun mistakesAreMostOverdueFirst() {
        val states = mapOf(state("q.001", 1, 90), state("q.002", 1, 100), state("q.003", 1, 95))

        val picked = QuestionPicker.mistakes(six, Level.JUNIOR, states, today, Random(3))

        assertEquals(listOf("q.001", "q.003", "q.002"), picked.map { it.id })
    }

    @Test
    fun mistakesExcludeQuestionsDueLater() {
        val states = mapOf(state("q.001", 1, 101), state("q.002", 1, 100))

        assertEquals(listOf("q.002"), QuestionPicker.mistakes(six, Level.JUNIOR, states, today, Random(3)).map { it.id })
    }

    @Test
    fun mistakesRespectTheLevel() {
        val mixed = six + single("s.001", Level.SENIOR)
        val states = mapOf(state("s.001", 1, 90), state("q.001", 1, 95))

        assertEquals(listOf("q.001"), QuestionPicker.mistakes(mixed, Level.JUNIOR, states, today, Random(3)).map { it.id })
        assertEquals(1, QuestionPicker.due(mixed, Level.JUNIOR, states, today).size)
        assertEquals(2, QuestionPicker.due(mixed, Level.SENIOR, states, today).size)
    }

    @Test
    fun mistakesAreCappedAtCount() {
        val many = (1..25).map { single("q.${it.toString().padStart(3, '0')}") }
        val states = many.associate { state(it.id, 1, 99) }

        assertEquals(20, QuestionPicker.mistakes(many, Level.JUNIOR, states, today, Random(3)).size)
    }
}
