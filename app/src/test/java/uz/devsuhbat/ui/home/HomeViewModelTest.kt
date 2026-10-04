package uz.devsuhbat.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.MockSummary
import uz.devsuhbat.data.ThemeMode
import uz.devsuhbat.data.UserSettings
import uz.devsuhbat.engine.NextReview
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionState

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = 100L

    private fun question(n: Int, level: String = "junior", topic: String = "a.t") = """
        {"id":"$topic.00$n","level":"$level","type":"single","prompt":"p",
         "options":[{"id":"a","text":"1","correct":true},{"id":"b","text":"2","hint":"h"}],"explanation":"e"}
    """.trimIndent()

    private val files = mapOf(
        "content/catalog.json" to """
            {"version":1,
             "fields":[{"id":"android","title":"Android","group":"mobile","topics":["a.t"]},
                       {"id":"ios","title":"iOS","group":"mobile","topics":["i.t"]},
                       {"id":"multi","title":"Multi","group":"mobile","topics":["a.t","a.u","a.e"]}],
             "topics":[{"id":"a.t","title":"T","file":"a_t.json"},{"id":"i.t","title":"I"},
                       {"id":"a.u","title":"U","file":"a_u.json"},{"id":"a.e","title":"E","file":"a_e.json"}]}
        """.trimIndent(),
        "content/questions/a_t.json" to
            """{"topic":"a.t","questions":[${question(1)},${question(2)},${question(3)},${question(4)},${question(5, "senior")}]}""",
        "content/questions/a_u.json" to
            """{"topic":"a.u","questions":[${question(1, topic = "a.u")},${question(2, topic = "a.u")}]}""",
        "content/questions/a_e.json" to
            """{"topic":"a.e","questions":[${question(1, "senior", topic = "a.e")}]}""",
    )
    private val content = ContentStore { files[it] }

    private val settings = MutableStateFlow(UserSettings("android", Level.JUNIOR, onboardingDone = true, theme = ThemeMode.SYSTEM))
    private val states = MutableStateFlow(
        mapOf(
            "a.t.001" to QuestionState("a.t.001", box = 3, dueDay = 200),
            "a.t.002" to QuestionState("a.t.002", box = 1, dueDay = 100),
            "a.t.005" to QuestionState("a.t.005", box = 1, dueDay = 90),
        )
    )

    private fun viewModel() = HomeViewModel(content, settings, states, { today }, dispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsReadinessAndDueCount() = runTest(dispatcher) {
        val state = viewModel().state.first { !it.loading }

        assertEquals("Android", state.fieldTitle)
        assertEquals(Level.JUNIOR, state.level)
        assertEquals(Progress(mastered = 1, total = 4), state.readiness)
        assertEquals(1, state.dueCount)
    }

    @Test
    fun showsWhenTheNextMistakesComeBack() = runTest(dispatcher) {
        states.value = mapOf("a.t.003" to QuestionState("a.t.003", box = 1, dueDay = 101))

        val state = viewModel().state.first { !it.loading }

        assertEquals(0, state.dueCount)
        assertEquals(NextReview(inDays = 1, count = 1), state.nextReview)
    }

    @Test
    fun higherLevelWidensTheScope() = runTest(dispatcher) {
        settings.value = settings.value.copy(level = Level.SENIOR)

        val state = viewModel().state.first { !it.loading }

        assertEquals(Progress(mastered = 1, total = 5), state.readiness)
        assertEquals(2, state.dueCount)
    }

    @Test
    fun updatesWhenStatesChange() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.first { !it.loading }

        states.value = states.value + ("a.t.002" to QuestionState("a.t.002", box = 3, dueDay = 107))

        val state = viewModel.state.first { it.dueCount == 0 }
        assertEquals(Progress(mastered = 2, total = 4), state.readiness)
    }

    @Test
    fun fieldWithoutContentIsAllZero() = runTest(dispatcher) {
        settings.value = settings.value.copy(fieldId = "ios")

        val state = viewModel().state.first { !it.loading }

        assertEquals("iOS", state.fieldTitle)
        assertEquals(Progress(0, 0), state.readiness)
        assertEquals(0, state.dueCount)
    }

    @Test
    fun mockQuestionCountIsTheEligibleCount() = runTest(dispatcher) {
        assertEquals(4, viewModel().state.first { !it.loading }.mockQuestionCount)

        settings.value = settings.value.copy(level = Level.SENIOR)

        assertEquals(5, viewModel().state.first { !it.loading }.mockQuestionCount)
    }

    @Test
    fun showsTheLastMockOfTheField() = runTest(dispatcher) {
        val viewModel = HomeViewModel(content, settings, states, { today }, dispatcher) { fieldId ->
            flowOf(MockSummary(correct = 18, total = 25).takeIf { fieldId == "android" })
        }

        assertEquals(MockSummary(18, 25), viewModel.state.first { !it.loading }.lastMock)

        settings.value = settings.value.copy(fieldId = "ios")

        assertEquals(null, viewModel.state.first { it.fieldTitle == "iOS" }.lastMock)
    }
    @Test
    fun streakComesFromSessionDays() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            content, settings, states, { today }, dispatcher,
            sessionDays = flowOf(listOf(today - 1, today - 1, today)),
        )

        assertEquals(2, viewModel.state.first { !it.loading }.streak.current)
    }

    @Test
    fun weakTopicsAreLowestFirst() = runTest(dispatcher) {
        settings.value = settings.value.copy(fieldId = "multi")

        val weak = viewModel().state.first { !it.loading }.weakTopics

        assertEquals(listOf("a.u", "a.t"), weak.map { it.topicId })
        assertEquals("U", weak.first().title)
    }

    @Test
    fun weakTopicsSkipMasteredAndEmptyTopics() = runTest(dispatcher) {
        settings.value = settings.value.copy(fieldId = "multi")
        fun mastered(id: String) = id to QuestionState(id, box = 3, dueDay = 200)
        states.value = states.value + mastered("a.u.001") + mastered("a.u.002")

        assertEquals(listOf("a.t"), viewModel().state.first { !it.loading }.weakTopics.map { it.topicId })

        states.value = listOf("a.t.001", "a.t.002", "a.t.003", "a.t.004", "a.u.001", "a.u.002").associate(::mastered)

        assertEquals(emptyList<TopicProgress>(), viewModel().state.first { !it.loading }.weakTopics)
    }
}
