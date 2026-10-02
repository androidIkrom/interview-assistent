package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.multi
import uz.devsuhbat.single

class GraderTest {

    private val single = single("t.001")
    private val multi = multi("t.002")

    private fun option(question: uz.devsuhbat.content.Question, id: String) = question.options.first { it.id == id }

    @Test
    fun singleCorrect() {
        assertEquals(Verdict.Correct, Grader.grade(single, setOf("a")))
    }

    @Test
    fun singleWrongCarriesHint() {
        val verdict = Grader.grade(single, setOf("c"))

        assertEquals(Verdict.Wrong(listOf(option(single, "c")), correctPicked = 0, correctMissing = 1), verdict)
        assertEquals("hint c", (verdict as Verdict.Wrong).wrongPicked.single().hint)
    }

    @Test
    fun multiAllCorrect() {
        assertEquals(Verdict.Correct, Grader.grade(multi, setOf("a", "b")))
    }

    @Test
    fun multiPartial() {
        assertEquals(Verdict.Wrong(emptyList(), correctPicked = 1, correctMissing = 1), Grader.grade(multi, setOf("a")))
    }

    @Test
    fun multiWithWrong() {
        assertEquals(
            Verdict.Wrong(listOf(option(multi, "c")), correctPicked = 1, correctMissing = 1),
            Grader.grade(multi, setOf("a", "c")),
        )
    }

    @Test
    fun multiOnlyWrong() {
        assertEquals(
            Verdict.Wrong(listOf(option(multi, "c"), option(multi, "d")), correctPicked = 0, correctMissing = 2),
            Grader.grade(multi, setOf("d", "c")),
        )
    }

    @Test
    fun multiAllCorrectPlusWrongIsWrong() {
        assertEquals(
            Verdict.Wrong(listOf(option(multi, "d")), correctPicked = 2, correctMissing = 0),
            Grader.grade(multi, setOf("a", "b", "d")),
        )
    }
}
