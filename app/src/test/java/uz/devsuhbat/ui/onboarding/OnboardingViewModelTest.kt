package uz.devsuhbat.ui.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val dataStoreScope = CoroutineScope(dispatcher + Job())

    private val files = mapOf(
        "content/catalog.json" to """
            {"version":1,
             "fields":[{"id":"android","title":"Android","group":"mobile","topics":["android.kotlin"]},
                       {"id":"ios","title":"iOS","group":"mobile","topics":["ios.swift"]},
                       {"id":"qa","title":"QA","group":"other","topics":[]}],
             "topics":[{"id":"android.kotlin","title":"Kotlin tili","file":"android_kotlin.json"},
                       {"id":"ios.swift","title":"Swift tili"}]}
        """.trimIndent(),
        "content/questions/android_kotlin.json" to """
            {"topic":"android.kotlin","questions":[{"id":"android.kotlin.001","level":"junior","type":"single",
            "prompt":"p","options":[{"id":"a","text":"1","correct":true},{"id":"b","text":"2","hint":"h"}],
            "explanation":"e"}]}
        """.trimIndent(),
    )
    private val content = ContentStore { files[it] }

    private val settings by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(tmp.root, "settings.preferences_pb") }
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        dataStoreScope.cancel()
    }

    private suspend fun loadedState(): OnboardingUiState =
        OnboardingViewModel(content, settings, dispatcher).state.first { !it.loading }

    @Test
    fun groupsFieldsInCatalogOrderAndMarksAvailability() = runTest(dispatcher) {
        val state = loadedState()

        assertEquals(listOf("mobile", "other"), state.groups.map { it.group })
        assertEquals(
            listOf("android" to true, "ios" to false, "qa" to false),
            state.groups.flatMap { it.rows }.map { it.field.id to it.available },
        )
    }

    @Test
    fun nothingIsSelectedOnFirstLaunch() = runTest(dispatcher) {
        val state = loadedState()

        assertNull(state.fieldId)
        assertNull(state.level)
    }

    @Test
    fun storedFieldWithoutContentStaysSelected() = runTest(dispatcher) {
        settings.completeOnboarding("ios", Level.MIDDLE)

        val state = loadedState()

        assertEquals("ios", state.fieldId)
        assertEquals(Level.MIDDLE, state.level)
    }

    @Test
    fun storedFieldMissingFromTheCatalogIsDropped() = runTest(dispatcher) {
        settings.completeOnboarding("cobol", Level.MIDDLE)

        assertNull(loadedState().fieldId)
    }

    @Test
    fun fieldWithoutContentCanBeChosenAndSaved() = runTest(dispatcher) {
        // One write per test: DataStore's rename-over-existing-file fails on a Windows JVM.
        val viewModel = OnboardingViewModel(content, settings, dispatcher)
        viewModel.state.first { !it.loading }
        val done = CompletableDeferred<Unit>()

        viewModel.selectField("ios")
        viewModel.showLevels()
        viewModel.selectLevel(Level.SENIOR)
        viewModel.confirm { done.complete(Unit) }

        assertEquals(OnboardingStep.LEVEL, viewModel.state.value.step)
        done.await()
        val stored = settings.settings.first()
        assertEquals("ios", stored.fieldId)
        assertEquals(Level.SENIOR, stored.level)
    }
}
