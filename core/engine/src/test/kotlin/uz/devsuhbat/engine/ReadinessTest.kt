package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.single

class ReadinessTest {

    private val questions = (1..4).map { single("t.00$it") }

    private fun states(vararg boxes: Pair<String, Int>) =
        boxes.associate { (id, box) -> id to QuestionState(id, box = box, dueDay = 0) }

    @Test
    fun countsMasteredQuestions() {
        val progress = Readiness.of(questions, states("t.001" to 3, "t.002" to 5, "t.003" to 2))

        assertEquals(Progress(mastered = 2, total = 4), progress)
        assertEquals(50, progress.percent)
    }

    @Test
    fun emptyPoolIsZeroPercent() {
        assertEquals(Progress(0, 0), Readiness.of(emptyList(), emptyMap()))
        assertEquals(0, Progress(0, 0).percent)
    }

    @Test
    fun ignoresStatesOfUnknownQuestions() {
        val progress = Readiness.of(questions, states("gone.001" to 5, "t.001" to 4))

        assertEquals(Progress(mastered = 1, total = 4), progress)
    }

    @Test
    fun percentRoundsDown() {
        assertEquals(33, Progress(1, 3).percent)
        assertEquals(100, Progress(3, 3).percent)
    }
}
