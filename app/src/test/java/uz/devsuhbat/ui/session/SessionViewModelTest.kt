package uz.devsuhbat.ui.session

import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.SessionResult

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun option(id: String, correct: Boolean = false) =
        Option(id, "text $id", correct, if (correct) null else "hint $id")

    private fun single(id: String) = Question(
        id = id, level = Level.JUNIOR, type = QuestionType.SINGLE, prompt = "prompt $id",
        options = listOf(option("a", true), option("b"), option("c"), option("d")),
        explanation = "explanation $id",
    )

    private fun multi(id: String) = Question(
        id = id, level = Level.JUNIOR, type = QuestionType.MULTI, prompt = "prompt $id",
        options = listOf(option("a", true), option("b", true), option("c"), option("d")),
        explanation = "explanation $id",
    )

    private val singles = listOf(single("t.001"), single("t.002"), single("t.003"))

    private fun viewModel(questions: List<Question> = singles) = SessionViewModel({ questions }, Random(1))

    private fun SessionViewModel.answerCorrectly() {
        toggle("a")
        check()
        next()
    }

    @Test
    fun loadsFirstQuestion() {
        val state = viewModel().state.value

        assertFalse(state.loading)
        assertEquals("t.001", state.question?.id)
        assertEquals(0, state.position)
        assertEquals(3, state.queueSize)
        assertFalse(state.canCheck)
        assertNull(state.feedback)
        assertNull(state.result)
    }

    @Test
    fun selectingEnablesCheck() {
        val viewModel = viewModel()

        viewModel.toggle("b")

        assertEquals(setOf("b"), viewModel.state.value.selected)
        assertTrue(viewModel.state.value.canCheck)
    }

    @Test
    fun singleSelectionReplacesThePreviousOne() {
        val viewModel = viewModel()

        viewModel.toggle("b")
        viewModel.toggle("c")

        assertEquals(setOf("c"), viewModel.state.value.selected)
    }

    @Test
    fun checkWithoutSelectionDoesNothing() {
        val viewModel = viewModel()

        viewModel.check()

        assertNull(viewModel.state.value.feedback)
        assertFalse(viewModel.state.value.solved)
    }

    @Test
    fun wrongAnswerShowsHintAndDisablesOption() {
        val viewModel = viewModel()

        viewModel.toggle("c")
        viewModel.check()

        val state = viewModel.state.value
        assertEquals(Feedback.Wrong(listOf("text c" to "hint c"), correctPicked = null, correctMissing = null), state.feedback)
        assertEquals(setOf("c"), state.disabled)
        assertEquals(emptySet<String>(), state.selected)
        assertFalse(state.solved)
        assertFalse(state.canCheck)
    }

    @Test
    fun wrongFeedbackNeverNamesTheCorrectOption() {
        val viewModel = viewModel()

        viewModel.toggle("c")
        viewModel.check()

        val wrong = viewModel.state.value.feedback as Feedback.Wrong
        assertTrue(wrong.hints.none { (text, hint) -> text == "text a" || "text a" in hint })
    }

    @Test
    fun correctAnswerShowsExplanation() {
        val viewModel = viewModel()

        viewModel.toggle("a")
        viewModel.check()

        val state = viewModel.state.value
        assertEquals(Feedback.Correct("explanation t.001"), state.feedback)
        assertTrue(state.solved)
        assertFalse(state.canCheck)
        assertEquals(setOf("a"), state.selected)
    }

    @Test
    fun nextAdvancesAndClearsFeedback() {
        val viewModel = viewModel()

        viewModel.answerCorrectly()

        val state = viewModel.state.value
        assertEquals("t.002", state.question?.id)
        assertEquals(1, state.position)
        assertNull(state.feedback)
        assertEquals(emptySet<String>(), state.selected)
        assertFalse(state.solved)
    }

    @Test
    fun nextBeforeSolvedDoesNothing() {
        val viewModel = viewModel()

        viewModel.next()

        assertEquals("t.001", viewModel.state.value.question?.id)
    }

    @Test
    fun multiTogglesOptionsOnAndOff() {
        val viewModel = viewModel(listOf(multi("t.001")))

        viewModel.toggle("a")
        viewModel.toggle("c")
        viewModel.toggle("a")

        assertEquals(setOf("c"), viewModel.state.value.selected)
    }

    @Test
    fun multiPartialReportsCounts() {
        val viewModel = viewModel(listOf(multi("t.001")))

        viewModel.toggle("a")
        viewModel.check()

        val state = viewModel.state.value
        assertEquals(Feedback.Wrong(emptyList(), correctPicked = 1, correctMissing = 1), state.feedback)
        assertEquals(setOf("a"), state.selected)
        assertFalse(state.solved)
    }

    @Test
    fun multiWrongPickIsRemovedFromSelection() {
        val viewModel = viewModel(listOf(multi("t.001")))

        viewModel.toggle("a")
        viewModel.toggle("d")
        viewModel.check()

        val state = viewModel.state.value
        assertEquals(Feedback.Wrong(listOf("text d" to "hint d"), correctPicked = 1, correctMissing = 1), state.feedback)
        assertEquals(setOf("a"), state.selected)
        assertEquals(setOf("d"), state.disabled)
    }

    @Test
    fun finishingProducesResult() {
        val viewModel = viewModel()

        repeat(3) { viewModel.answerCorrectly() }

        val state = viewModel.state.value
        assertEquals(SessionResult(total = 3, firstTryCorrect = 3, reworked = 0), state.result)
        assertNull(state.question)
    }

    @Test
    fun missedQuestionComesBackAsRepeat() {
        val viewModel = viewModel()

        viewModel.toggle("c")
        viewModel.check()
        viewModel.answerCorrectly()
        viewModel.answerCorrectly()
        viewModel.answerCorrectly()

        val state = viewModel.state.value
        assertTrue(state.isRepeat)
        assertEquals("t.001", state.question?.id)
        assertEquals(4, state.queueSize)
        assertEquals(emptySet<String>(), state.disabled)

        viewModel.answerCorrectly()

        assertEquals(SessionResult(total = 3, firstTryCorrect = 2, reworked = 1), viewModel.state.value.result)
    }

    @Test
    fun emptyPoolFinishesImmediately() {
        val state = viewModel(emptyList()).state.value

        assertFalse(state.loading)
        assertEquals(SessionResult(total = 0, firstTryCorrect = 0, reworked = 0), state.result)
    }

    @Test
    fun toggleIgnoredForDisabledOption() {
        val viewModel = viewModel()
        viewModel.toggle("c")
        viewModel.check()

        viewModel.toggle("c")

        assertEquals(emptySet<String>(), viewModel.state.value.selected)
        assertNotNull(viewModel.state.value.feedback)
    }

    @Test
    fun toggleIgnoredAfterSolved() {
        val viewModel = viewModel()
        viewModel.toggle("a")
        viewModel.check()

        viewModel.toggle("b")

        assertEquals(setOf("a"), viewModel.state.value.selected)
    }
}
