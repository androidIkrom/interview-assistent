package uz.devsuhbat.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.multi
import uz.devsuhbat.single

class MockSessionTest {

    private val questions = listOf(single("a.x.001"), single("a.x.002"), multi("a.y.001"))

    private fun session() = MockSession(questions, Random(1))

    @Test
    fun keepsQuestionOrderAndAllOptions() {
        val session = session()

        assertEquals(3, session.size)
        assertEquals(listOf("a.x.001", "a.x.002", "a.y.001"), session.questions.map { it.id })
        session.questions.forEach { assertEquals(setOf("a", "b", "c", "d"), it.options.map { o -> o.id }.toSet()) }
    }

    @Test
    fun answerIsStoredAndCanBeChanged() {
        val session = session()

        session.answer(0, setOf("b"))
        session.answer(0, setOf("a"))

        assertEquals(setOf("a"), session.selection(0))
        assertEquals(emptySet<String>(), session.selection(1))
        assertEquals(1, session.answeredCount)
    }

    @Test
    fun emptyAnswerClears() {
        val session = session()
        session.answer(0, setOf("b"))

        session.answer(0, emptySet())

        assertEquals(0, session.answeredCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownOptionIsRejected() {
        session().answer(0, setOf("z"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun twoOptionsForSingleAreRejected() {
        session().answer(0, setOf("a", "b"))
    }

    @Test
    fun finishScoresAnswers() {
        val session = session()
        session.answer(0, setOf("a"))
        session.answer(1, setOf("c"))

        val result = session.finish()

        assertEquals(3, result.total)
        assertEquals(1, result.correct)
        assertEquals(listOf(TopicScore("a.x", total = 2, correct = 1), TopicScore("a.y", total = 1, correct = 0)), result.topics)
        assertEquals(
            listOf(
                MockAnswer("a.x.001", "a.x", correct = true),
                MockAnswer("a.x.002", "a.x", correct = false),
                MockAnswer("a.y.001", "a.y", correct = false),
            ),
            result.answers,
        )
    }

    @Test
    fun multiNeedsTheExactSet() {
        fun correctWith(selected: Set<String>): Boolean {
            val session = session()
            session.answer(2, selected)
            return session.finish().answers[2].correct
        }

        assertEquals(false, correctWith(setOf("a")))
        assertEquals(true, correctWith(setOf("a", "b")))
        assertEquals(false, correctWith(setOf("a", "b", "c")))
    }

    @Test
    fun finishIsRepeatable() {
        val session = session()
        session.answer(0, setOf("a"))

        assertEquals(session.finish(), session.finish())
    }

    @Test
    fun emptyMockHasAnEmptyResult() {
        val result = MockSession(emptyList(), Random(1)).finish()

        assertEquals(0, result.total)
        assertEquals(emptyList<TopicScore>(), result.topics)
    }
}
