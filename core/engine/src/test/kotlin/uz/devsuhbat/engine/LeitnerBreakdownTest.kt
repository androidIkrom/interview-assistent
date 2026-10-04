package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType

class LeitnerBreakdownTest {
    private fun question(id: String) = Question(
        id = id, level = Level.JUNIOR, type = QuestionType.SINGLE, prompt = "p",
        options = listOf(Option("a", "1", true, null), Option("b", "2", false, "h")),
        explanation = "e",
    )

    private val questions = (1..5).map { question("q$it") }

    @Test
    fun splitsQuestionsIntoThreeGroups() {
        val states = mapOf(
            "q1" to QuestionState("q1", box = 1, dueDay = 0),
            "q2" to QuestionState("q2", box = 2, dueDay = 0),
            "q3" to QuestionState("q3", box = 3, dueDay = 0),
            "q4" to QuestionState("q4", box = 5, dueDay = 0),
        )
        val breakdown = LeitnerBreakdown.of(questions, states)
        assertEquals(Breakdown(fresh = 1, learning = 2, mastered = 2), breakdown)
        assertEquals(5, breakdown.total)
    }

    @Test
    fun statesOfOtherQuestionsAreIgnored() {
        val states = mapOf("other" to QuestionState("other", box = 5, dueDay = 0))
        assertEquals(Breakdown(fresh = 5, learning = 0, mastered = 0), LeitnerBreakdown.of(questions, states))
    }

    @Test
    fun noQuestionsIsAllZero() {
        assertEquals(Breakdown(0, 0, 0), LeitnerBreakdown.of(emptyList(), emptyMap()))
    }
}
