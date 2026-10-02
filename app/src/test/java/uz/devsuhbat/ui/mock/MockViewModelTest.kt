package uz.devsuhbat.ui.mock

import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.MockResult

@OptIn(ExperimentalCoroutinesApi::class)
class MockViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val finished = mutableListOf<MockResult>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
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

    private val three = listOf(single("a.x.001"), single("a.x.002"), multi("a.y.001"))
    private val titles = mapOf("a.x" to "X mavzu", "a.y" to "Y mavzu")

    private fun TestScope.viewModel(
        questions: List<Question> = three,
        now: () -> Long = { testScheduler.currentTime },
    ): MockViewModel {
        val viewModel = MockViewModel(
            load = { MockSetup(questions, titles) },
            random = Random(1),
            now = now,
            onFinished = { finished += it },
            durationSeconds = 60,
        )
        runCurrent()
        return viewModel
    }

    @Test
    fun loadsFirstQuestionWithFullTime() = runTest(dispatcher) {
        val state = viewModel().state.value

        assertFalse(state.loading)
        assertEquals("a.x.001", state.question?.id)
        assertEquals(0, state.index)
        assertEquals(3, state.size)
        assertEquals(60, state.remainingSeconds)
        assertEquals(titles, state.topicTitles)
        assertNull(state.result)
    }

    @Test
    fun selectionIsKeptPerQuestion() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.toggle("a")
        viewModel.next()
        assertEquals("a.x.002", viewModel.state.value.question?.id)
        assertEquals(emptySet<String>(), viewModel.state.value.selected)
        viewModel.previous()

        assertEquals(setOf("a"), viewModel.state.value.selected)
        assertEquals(setOf(0), viewModel.state.value.answered)
    }

    @Test
    fun tappingTheSelectedSingleOptionClearsIt() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.toggle("b")
        viewModel.toggle("b")

        assertEquals(emptySet<String>(), viewModel.state.value.selected)
        assertEquals(emptySet<Int>(), viewModel.state.value.answered)
    }

    @Test
    fun multiTogglesEachOption() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.goTo(2)

        viewModel.toggle("a")
        viewModel.toggle("b")
        viewModel.toggle("a")

        assertEquals(setOf("b"), viewModel.state.value.selected)
    }

    @Test
    fun navigationStaysInRange() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.previous()
        assertEquals(0, viewModel.state.value.index)
        viewModel.goTo(7)
        assertEquals(0, viewModel.state.value.index)
        viewModel.goTo(2)
        viewModel.next()

        assertEquals(2, viewModel.state.value.index)
    }

    @Test
    fun finishScoresAndReports() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle("a")
        viewModel.next()
        viewModel.toggle("c")

        viewModel.finish()
        runCurrent()

        val result = viewModel.state.value.result
        assertNotNull(result)
        assertEquals(3, result!!.total)
        assertEquals(1, result.correct)
        assertEquals(listOf(result), finished)
    }

    @Test
    fun resultIsMarkedStoredOnlyAfterItWasReported() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.finish()
        assertNotNull(viewModel.state.value.result)
        assertFalse(viewModel.state.value.stored)
        runCurrent()

        assertEquals(1, finished.size)
        assertEquals(true, viewModel.state.value.stored)
    }

    @Test
    fun finishIsReportedOnce() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.finish()
        viewModel.finish()
        advanceTimeBy(120_000)
        runCurrent()

        assertEquals(1, finished.size)
    }

    @Test
    fun answersCannotChangeAfterFinish() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.finish()
        val result = viewModel.state.value.result

        viewModel.toggle("a")

        assertEquals(result, viewModel.state.value.result)
    }

    @Test
    fun timeRunningOutFinishesTheMock() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.toggle("a")

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, viewModel.state.value.remainingSeconds)
        assertNull(viewModel.state.value.result)

        advanceTimeBy(31_000)
        runCurrent()

        assertEquals(0, viewModel.state.value.remainingSeconds)
        assertEquals(1, viewModel.state.value.result?.correct)
        assertEquals(1, finished.size)
    }

    @Test
    fun remainingTimeFollowsTheClockNotTheTicks() = runTest(dispatcher) {
        var clock = 0L
        val viewModel = viewModel(now = { clock })
        assertEquals(60, viewModel.state.value.remainingSeconds)

        // The app sat in the background: one tick fires, but 40 seconds have passed on the clock.
        clock = 40_000
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(20, viewModel.state.value.remainingSeconds)

        // This clock never reaches the deadline, so the timer would tick forever while runTest drains the scheduler.
        viewModel.finish()
    }

    @Test
    fun emptySetupFinishesWithoutReporting() = runTest(dispatcher) {
        val viewModel = viewModel(questions = emptyList())
        runCurrent()

        assertEquals(0, viewModel.state.value.result?.total)
        assertEquals(emptyList<MockResult>(), finished)
    }
}
