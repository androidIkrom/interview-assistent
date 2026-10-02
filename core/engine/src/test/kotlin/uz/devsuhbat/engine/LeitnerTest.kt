package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerTest {

    private val today = 100L
    private val id = "t.001"

    private fun firstTry() = QuestionOutcome(id, firstTryCorrect = true, wrongSubmissions = 0)
    private fun missed(wrong: Int = 1) = QuestionOutcome(id, firstTryCorrect = false, wrongSubmissions = wrong)
    private fun state(box: Int, attempts: Int = 1, wrong: Int = 0) =
        QuestionState(id, box = box, dueDay = today, attempts = attempts, wrongAttempts = wrong)

    @Test
    fun intervals() {
        assertEquals(listOf(1L, 3L, 7L, 14L, 30L), (1..5).map { Leitner.intervalDays(it) })
    }

    @Test(expected = IllegalArgumentException::class)
    fun intervalOfUnknownBoxThrows() {
        Leitner.intervalDays(0)
    }

    @Test
    fun newQuestionFirstTryGoesToBoxThree() {
        assertEquals(
            QuestionState(id, box = 3, dueDay = 107, attempts = 1, wrongAttempts = 0),
            Leitner.afterOutcome(null, firstTry(), today),
        )
    }

    @Test
    fun newQuestionMissedGoesToBoxOne() {
        assertEquals(
            QuestionState(id, box = 1, dueDay = 101, attempts = 1, wrongAttempts = 2),
            Leitner.afterOutcome(null, missed(wrong = 2), today),
        )
    }

    @Test
    fun wrongAnswerIsDueTomorrow() {
        val after = Leitner.afterOutcome(state(box = 4), missed(), today)

        assertEquals(1, after.box)
        assertEquals(101, after.dueDay)
        assertFalse(after.isDue(today))
        assertTrue(after.isDue(today + 1))
    }

    @Test
    fun seenQuestionFirstTryMovesUpOneBox() {
        val fromOne = Leitner.afterOutcome(state(box = 1), firstTry(), today)
        val fromThree = Leitner.afterOutcome(state(box = 3), firstTry(), today)

        assertEquals(2 to 103L, fromOne.box to fromOne.dueDay)
        assertEquals(4 to 114L, fromThree.box to fromThree.dueDay)
    }

    @Test
    fun boxNeverExceedsFive() {
        val after = Leitner.afterOutcome(state(box = 5), firstTry(), today)

        assertEquals(5 to 130L, after.box to after.dueDay)
    }

    @Test
    fun countersAccumulate() {
        val after = Leitner.afterOutcome(state(box = 2, attempts = 3, wrong = 2), missed(wrong = 1), today)

        assertEquals(4, after.attempts)
        assertEquals(3, after.wrongAttempts)
    }

    @Test
    fun masteredFromBoxThree() {
        assertFalse(state(box = 2).mastered)
        assertTrue(state(box = 3).mastered)
        assertTrue(state(box = 5).mastered)
    }

    @Test
    fun afterMockCorrectBehavesLikeFirstTry() {
        assertEquals(Leitner.afterOutcome(null, firstTry(), today), Leitner.afterMock(null, id, correct = true, today = today))
        assertEquals(
            Leitner.afterOutcome(state(box = 1), firstTry(), today),
            Leitner.afterMock(state(box = 1), id, correct = true, today = today),
        )
    }

    @Test
    fun afterMockWrongIsDueToday() {
        val after = Leitner.afterMock(state(box = 4, attempts = 2, wrong = 0), id, correct = false, today = today)

        assertEquals(QuestionState(id, box = 1, dueDay = 100, attempts = 3, wrongAttempts = 1), after)
        assertTrue(after.isDue(today))
    }

    @Test
    fun afterMockWrongOnNewQuestionIsDueToday() {
        assertEquals(
            QuestionState(id, box = 1, dueDay = 100, attempts = 1, wrongAttempts = 1),
            Leitner.afterMock(null, id, correct = false, today = today),
        )
    }
}
