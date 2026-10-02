package uz.devsuhbat.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.devsuhbat.single
import uz.devsuhbat.trueFalse

class PracticeSessionTest {

    private val questions = listOf(single("t.001"), single("t.002"), single("t.003"))

    private fun session() = PracticeSession(questions, Random(1))

    private fun PracticeSession.answerCorrectly() {
        submit(setOf("a"))
        next()
    }

    private fun PracticeSession.missThenSolve() {
        submit(setOf("c"))
        submit(setOf("a"))
        next()
    }

    @Test
    fun startsAtTheFirstQuestion() {
        val session = session()

        assertEquals(3, session.total)
        assertEquals(3, session.queueSize)
        assertEquals(0, session.position)
        assertEquals("t.001", session.attempt!!.question.id)
        assertFalse(session.isRepeat)
        assertFalse(session.finished)
    }

    @Test
    fun allFirstTryFinishesWithoutRepeats() {
        val session = session()

        repeat(3) { session.answerCorrectly() }

        assertTrue(session.finished)
        assertNull(session.attempt)
        assertEquals(3, session.queueSize)
        assertEquals(SessionResult(total = 3, firstTryCorrect = 3, reworked = 0), session.result())
    }

    @Test
    fun missedQuestionIsRepeatedAtTheEnd() {
        val session = session()

        session.missThenSolve()
        assertEquals(4, session.queueSize)
        session.answerCorrectly()
        session.answerCorrectly()

        assertFalse(session.finished)
        assertTrue(session.isRepeat)
        assertEquals(3, session.position)
        assertEquals("t.001", session.attempt!!.question.id)
        assertEquals(emptySet<String>(), session.attempt!!.disabled)

        session.answerCorrectly()

        assertTrue(session.finished)
    }

    @Test
    fun repeatIsNotRepeatedAgain() {
        val session = session()

        session.missThenSolve()
        session.answerCorrectly()
        session.answerCorrectly()
        session.missThenSolve()

        assertTrue(session.finished)
        assertEquals(4, session.queueSize)
    }

    @Test
    fun outcomesRecordFirstPresentationOnly() {
        val session = session()

        session.missThenSolve()
        session.answerCorrectly()
        session.answerCorrectly()
        session.answerCorrectly()

        assertEquals(
            listOf(
                QuestionOutcome("t.001", firstTryCorrect = false, wrongSubmissions = 1),
                QuestionOutcome("t.002", firstTryCorrect = true, wrongSubmissions = 0),
                QuestionOutcome("t.003", firstTryCorrect = true, wrongSubmissions = 0),
            ),
            session.outcomes,
        )
    }

    @Test
    fun outcomeIsAvailableAsSoonAsTheQuestionIsSolved() {
        val session = session()

        session.submit(setOf("a"))

        assertEquals(listOf(QuestionOutcome("t.001", firstTryCorrect = true, wrongSubmissions = 0)), session.outcomes)
    }

    @Test
    fun resultCountsFirstTryCorrect() {
        val session = session()

        session.missThenSolve()
        session.answerCorrectly()
        session.answerCorrectly()
        session.answerCorrectly()

        assertEquals(SessionResult(total = 3, firstTryCorrect = 2, reworked = 1), session.result())
    }

    @Test
    fun optionsAreShuffledButComplete() {
        val session = session()
        val orders = mutableListOf<List<String>>()

        repeat(3) {
            orders += session.attempt!!.question.options.map { it.id }
            session.answerCorrectly()
        }

        orders.forEach { assertEquals(setOf("a", "b", "c", "d"), it.toSet()) }
        assertTrue("at least one question should be reordered: $orders", orders.any { it != listOf("a", "b", "c", "d") })
    }

    @Test
    fun trueFalseKeepsOptionOrder() {
        for (seed in 0..20) {
            val session = PracticeSession(listOf(trueFalse("t.001")), Random(seed))

            assertEquals(listOf("a", "b"), session.attempt!!.question.options.map { it.id })
        }
    }

    @Test(expected = IllegalStateException::class)
    fun nextBeforeSolvedThrows() {
        session().next()
    }

    @Test(expected = IllegalStateException::class)
    fun resultBeforeFinishedThrows() {
        session().result()
    }

    @Test
    fun emptySessionIsFinishedImmediately() {
        val session = PracticeSession(emptyList(), Random(1))

        assertTrue(session.finished)
        assertNull(session.attempt)
        assertEquals(SessionResult(total = 0, firstTryCorrect = 0, reworked = 0), session.result())
    }
}
