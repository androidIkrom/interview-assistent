package uz.devsuhbat.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import uz.devsuhbat.data.ThemeMode
import uz.devsuhbat.data.UserSettings
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionState

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = 100L

    private fun question(n: Int, level: String = "junior") = """
        {"id":"a.t.00$n","level":"$level","type":"single","prompt":"p",
         "options":[{"id":"a","text":"1","correct":true},{"id":"b","text":"2","hint":"h"}],"explanation":"e"}
    """.trimIndent()

    private val files = mapOf(
        "content/catalog.json" to """
            {"version":1,
             "fields":[{"id":"android","title":"Android","group":"mobile","topics":["a.t"]},
                       {"id":"ios","title":"iOS","group":"mobile","topics":["i.t"]}],
             "topics":[{"id":"a.t","title":"T","file":"a_t.json"},{"id":"i.t","title":"I"}]}
        """.trimIndent(),
        "content/questions/a_t.json" to
            """{"topic":"a.t","questions":[${question(1)},${question(2)},${question(3)},${question(4)},${question(5, "senior")}]}""",
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
}
