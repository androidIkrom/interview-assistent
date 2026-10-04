package uz.devsuhbat.ui.stats

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.MockSummary
import uz.devsuhbat.data.ThemeMode
import uz.devsuhbat.data.UserSettings
import uz.devsuhbat.engine.ActivityGrid
import uz.devsuhbat.engine.Breakdown
import uz.devsuhbat.engine.QuestionState

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = 20_000L

    private fun question(id: String, level: String = "junior") = """
        {"id":"$id","level":"$level","type":"single","prompt":"p",
         "options":[{"id":"a","text":"1","correct":true},{"id":"b","text":"2","hint":"h"}],"explanation":"e"}
    """.trimIndent()

    private fun topicFile(topic: String, ids: List<String>) =
        """{"topic":"$topic","questions":[${ids.joinToString(",") { question(it) }}]}"""

    private val files = mapOf(
        "content/catalog.json" to """
            {"version":1,
             "fields":[{"id":"android","title":"Android","group":"mobile","topics":["a.t","a.u"]},
                       {"id":"multi","title":"Multi","group":"mobile","topics":["m.t"]}],
             "topics":[{"id":"a.t","title":"T","file":"a_t.json"},{"id":"a.u","title":"U","file":"a_u.json"},
                       {"id":"m.t","title":"M","file":"m_t.json"}]}
        """.trimIndent(),
        "content/questions/a_t.json" to topicFile("a.t", listOf("a.t.1", "a.t.2", "a.t.3", "a.t.4")),
        "content/questions/a_u.json" to topicFile("a.u", listOf("a.u.1", "a.u.2")),
        "content/questions/m_t.json" to topicFile("m.t", listOf("m.t.1", "m.t.2", "m.t.3", "m.t.4", "m.t.5", "m.t.6")),
    )
    private val content = ContentStore { files[it] }

    private val settings = MutableStateFlow(UserSettings("android", Level.JUNIOR, onboardingDone = true, theme = ThemeMode.SYSTEM))
    private val states = MutableStateFlow(
        mapOf(
            "a.t.1" to QuestionState("a.t.1", box = 3, dueDay = today + 7),
            "a.t.2" to QuestionState("a.t.2", box = 1, dueDay = today),
            "a.u.1" to QuestionState("a.u.1", box = 3, dueDay = today + 7),
            "a.u.2" to QuestionState("a.u.2", box = 4, dueDay = today + 14),
        )
    )
    private val mocks = mapOf(
        "android" to listOf(MockSummary(18, 25), MockSummary(20, 25)),
        "multi" to listOf(MockSummary(5, 25)),
    )

    private fun viewModel(
        sessionDays: List<Long> = listOf(today - 1, today),
        mockHistory: (String) -> List<MockSummary> = { mocks[it].orEmpty() },
    ) = StatsViewModel(
        content, settings, states, flowOf(sessionDays), { today }, dispatcher,
    ) { fieldId -> flowOf(mockHistory(fieldId)) }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun combinesStreakGridBreakdownAndTopics() = runTest(dispatcher) {
        val state = viewModel().state.first { !it.loading }

        assertEquals("Android", state.fieldTitle)
        assertEquals(2, state.streak.current)
        assertEquals(ActivityGrid.WEEKS, state.grid.size)
        assertEquals(Breakdown(fresh = 2, learning = 1, mastered = 3), state.breakdown)
        assertEquals(listOf("a.t", "a.u"), state.topics.map { it.topicId })
        assertEquals(listOf(MockSummary(18, 25), MockSummary(20, 25)), state.mocks)
        assertTrue(state.mockAvailable)
    }

    @Test
    fun switchingFieldKeepsActivityButChangesTheRest() = runTest(dispatcher) {
        val viewModel = viewModel()
        val before = viewModel.state.first { !it.loading }

        settings.value = settings.value.copy(fieldId = "multi")
        val after = viewModel.state.first { it.fieldTitle == "Multi" }

        assertEquals(before.streak, after.streak)
        assertEquals(before.grid, after.grid)
        assertEquals(listOf("m.t"), after.topics.map { it.topicId })
        assertEquals(Breakdown(fresh = 6, learning = 0, mastered = 0), after.breakdown)
        assertEquals(listOf(MockSummary(5, 25)), after.mocks)
    }

    @Test
    fun newUserGetsEmptyButValidStats() = runTest(dispatcher) {
        states.value = emptyMap()
        val state = viewModel(sessionDays = emptyList(), mockHistory = { emptyList() }).state.first { !it.loading }

        assertEquals(0, state.streak.current)
        assertEquals(0, state.grid.flatten().sumOf { it ?: 0 })
        assertEquals(state.breakdown.total, state.breakdown.fresh)
        assertTrue(state.topics.all { it.progress.percent == 0 })
        assertTrue(state.mocks.isEmpty())
        assertTrue(state.mockAvailable)
    }
}
