package uz.devsuhbat.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // --- next review ---

    @Test
    fun nextReviewIsTheEarliestFutureDay() {
        val states = mapOf(state("j.001", 1, 102), state("j.002", 2, 102), state("j.003", 3, 105))

        assertEquals(NextReview(inDays = 2, count = 2), QuestionPicker.nextReview(pool, Level.JUNIOR, states, today))
    }

    @Test
    fun nextReviewIgnoresDueAndOutOfLevelQuestions() {
        val states = mapOf(state("j.001", 1, 99), state("x.001", 1, 101))

        assertNull(QuestionPicker.nextReview(pool, Level.JUNIOR, states, today))
    }

    // --- mock pick ---

    private fun many(prefix: String, level: Level, count: Int) =
        (1..count).map { single("$prefix.${it.toString().padStart(3, '0')}", level) }

    private fun List<uz.devsuhbat.content.Question>.countOf(level: Level) = count { it.level == level }

    @Test
    fun mockMixesTargetAndLowerLevels() {
        val picked = QuestionPicker.mock(many("m", Level.MIDDLE, 30) + many("j", Level.JUNIOR, 30), Level.MIDDLE, Random(5))

        assertEquals(25, picked.size)
        assertEquals(15, picked.countOf(Level.MIDDLE))
        assertEquals(10, picked.countOf(Level.JUNIOR))
    }

    @Test
    fun mockFillsFromLowerWhenTargetIsShort() {
        val picked = QuestionPicker.mock(many("m", Level.MIDDLE, 5) + many("j", Level.JUNIOR, 30), Level.MIDDLE, Random(5))

        assertEquals(5, picked.countOf(Level.MIDDLE))
        assertEquals(20, picked.countOf(Level.JUNIOR))
    }

    @Test
    fun mockFillsFromTargetWhenLowerIsShort() {
        val picked = QuestionPicker.mock(many("m", Level.MIDDLE, 30) + many("j", Level.JUNIOR, 3), Level.MIDDLE, Random(5))

        assertEquals(22, picked.countOf(Level.MIDDLE))
        assertEquals(3, picked.countOf(Level.JUNIOR))
    }

    @Test
    fun mockForJuniorIsAllJunior() {
        val picked = QuestionPicker.mock(many("j", Level.JUNIOR, 30) + many("m", Level.MIDDLE, 30), Level.JUNIOR, Random(5))

        assertEquals(25, picked.countOf(Level.JUNIOR))
    }

    @Test
    fun mockReturnsAllWhenThePoolIsSmall() {
        val small = many("m", Level.MIDDLE, 4) + many("j", Level.JUNIOR, 3)

        val picked = QuestionPicker.mock(small, Level.MIDDLE, Random(5))

        assertEquals(small.map { it.id }.toSet(), picked.map { it.id }.toSet())
        assertEquals(7, picked.size)
    }

    private fun List<uz.devsuhbat.content.Question>.fieldCount() = count { !it.id.startsWith("core.") }

    @Test
    fun mockTakesSixtyPercentFromFieldTopics() {
        val picked = QuestionPicker.mock(many("f", Level.MIDDLE, 40) + many("core.x", Level.MIDDLE, 40), Level.MIDDLE, Random(3))

        assertEquals(25, picked.size)
        assertEquals(15, picked.fieldCount())
    }

    @Test
    fun mockFillsFromCommonWhenFieldIsShort() {
        val picked = QuestionPicker.mock(many("f", Level.MIDDLE, 6) + many("core.x", Level.MIDDLE, 40), Level.MIDDLE, Random(3))

        assertEquals(25, picked.size)
        assertEquals(6, picked.fieldCount())
    }

    @Test
    fun mockFillsFromFieldWhenCommonIsShort() {
        val picked = QuestionPicker.mock(many("f", Level.MIDDLE, 40) + many("core.x", Level.MIDDLE, 4), Level.MIDDLE, Random(3))

        assertEquals(25, picked.size)
        assertEquals(21, picked.fieldCount())
    }

    @Test
    fun mockKeepsLevelMixInsideEachPart() {
        val pool = many("f.m", Level.MIDDLE, 30) + many("f.j", Level.JUNIOR, 30) +
            many("core.m", Level.MIDDLE, 30) + many("core.j", Level.JUNIOR, 30)

        val picked = QuestionPicker.mock(pool, Level.MIDDLE, Random(3))

        assertEquals(15, picked.countOf(Level.MIDDLE))
        assertEquals(15, picked.fieldCount())
    }

    @Test
    fun mockHasNoDuplicates() {
        repeat(10) { seed ->
            val picked = QuestionPicker.mock(many("m", Level.MIDDLE, 20) + many("j", Level.JUNIOR, 20), Level.MIDDLE, Random(seed))

            assertEquals(picked.size, picked.map { it.id }.toSet().size)
        }
    }
}
