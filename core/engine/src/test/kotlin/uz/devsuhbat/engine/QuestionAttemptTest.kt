package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.devsuhbat.multi
import uz.devsuhbat.single

class QuestionAttemptTest {

    private val fresh = QuestionAttempt(single("t.001"))
    private val freshMulti = QuestionAttempt(multi("t.002"))

    @Test
    fun firstTryCorrect() {
        val (attempt, verdict) = fresh.submit(setOf("a"))

        assertEquals(Verdict.Correct, verdict)
        assertTrue(attempt.solved)
        assertTrue(attempt.firstTryCorrect)
        assertEquals(0, attempt.wrongSubmissions)
    }

    @Test
    fun wrongThenCorrectIsNotFirstTry() {
        val (afterWrong, wrong) = fresh.submit(setOf("c"))

        assertTrue(wrong is Verdict.Wrong)
        assertEquals(setOf("c"), afterWrong.disabled)
        assertEquals(1, afterWrong.wrongSubmissions)
        assertFalse(afterWrong.solved)
        assertFalse(afterWrong.firstTryCorrect)

        val (afterCorrect, correct) = afterWrong.submit(setOf("a"))

        assertEquals(Verdict.Correct, correct)
        assertTrue(afterCorrect.solved)
        assertFalse(afterCorrect.firstTryCorrect)
        assertEquals(1, afterCorrect.wrongSubmissions)
    }

    @Test
    fun unsolvedAttemptIsNotFirstTryCorrect() {
        assertFalse(fresh.firstTryCorrect)
    }

    @Test
    fun cannotSubmitEmptySelection() {
        assertFalse(fresh.canSubmit(emptySet()))
    }

    @Test
    fun cannotSubmitDisabledOption() {
        val (afterWrong, _) = fresh.submit(setOf("c"))

        assertFalse(afterWrong.canSubmit(setOf("c")))
        assertTrue(afterWrong.canSubmit(setOf("b")))
    }

    @Test
    fun cannotSubmitUnknownOption() {
        assertFalse(fresh.canSubmit(setOf("z")))
    }

    @Test
    fun cannotSubmitTwoOptionsForSingle() {
        assertFalse(fresh.canSubmit(setOf("a", "b")))
        assertTrue(freshMulti.canSubmit(setOf("a", "b")))
    }

    @Test
    fun cannotSubmitAfterSolved() {
        val (solved, _) = fresh.submit(setOf("a"))

        assertFalse(solved.canSubmit(setOf("b")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun submitThrowsWhenNotAllowed() {
        fresh.submit(emptySet())
    }

    @Test
    fun multiPartialDisablesNothingButCountsAsWrong() {
        val (attempt, verdict) = freshMulti.submit(setOf("a"))

        assertEquals(Verdict.Wrong(emptyList(), correctPicked = 1, correctMissing = 1), verdict)
        assertEquals(emptySet<String>(), attempt.disabled)
        assertEquals(1, attempt.wrongSubmissions)
        assertFalse(attempt.solved)
    }

    @Test
    fun multiWrongOptionsAreDisabled() {
        val (attempt, _) = freshMulti.submit(setOf("a", "c"))

        assertEquals(setOf("c"), attempt.disabled)

        val (solved, verdict) = attempt.submit(setOf("a", "b"))

        assertEquals(Verdict.Correct, verdict)
        assertTrue(solved.solved)
        assertFalse(solved.firstTryCorrect)
    }
}
