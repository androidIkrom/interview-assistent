package uz.devsuhbat.ui.mock

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.MockResult
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.ui.common.QuestionBody
import uz.devsuhbat.ui.design.OptionCard
import uz.devsuhbat.ui.design.OptionState
import uz.devsuhbat.ui.common.ReportIssueAction

private const val LOW_TIME_SECONDS = 60

private suspend fun loadMock(container: AppContainer, random: Random): MockSetup {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId
    val level = settings.level
    if (fieldId == null || level == null) return MockSetup(emptyList(), emptyMap())
    return withContext(container.io) {
        val topics = container.content.topicsOf(fieldId)
        val pool = topics.flatMap { container.content.questions(it.id) }
        MockSetup(QuestionPicker.mock(pool, level, random), topics.associate { it.id to it.title })
    }
}

private suspend fun storeMock(container: AppContainer, startedAt: Instant, result: MockResult) {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return
    val level = settings.level ?: return
    container.progress.recordMock(fieldId, level, startedAt, result)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockScreen(container: AppContainer, onExit: () -> Unit, onMistakes: () -> Unit, onHome: () -> Unit) {
    val viewModel: MockViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val random = Random.Default
                val startedAt = Instant.now()
                MockViewModel(
                    load = { loadMock(container, random) },
                    random = random,
                    // Monotonic and counts time spent asleep, unlike the wall clock the user can change.
                    now = SystemClock::elapsedRealtime,
                    onFinished = { result -> storeMock(container, startedAt, result) },
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result

    if (result != null) {
        BackHandler(onBack = onHome)
        MockResultScreen(
            result = result,
            topicTitles = state.topicTitles,
            stored = state.stored,
            onMistakes = onMistakes,
            onHome = onHome,
        )
        return
    }

    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmExit = true }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.mock_exit_title)) },
            text = { Text(stringResource(R.string.mock_exit_text)) },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onExit() }) {
                    Text(stringResource(R.string.session_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.action_continue)) }
            },
        )
    }

    if (confirmFinish) {
        val unanswered = state.size - state.answered.size
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.mock_finish_title)) },
            text = {
                Text(
                    if (unanswered > 0) stringResource(R.string.mock_finish_unanswered, unanswered)
                    else stringResource(R.string.mock_finish_all_answered)
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; viewModel.finish() }) {
                    Text(stringResource(R.string.session_finish))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.action_continue)) }
            },
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = formatTime(state.remainingSeconds),
                            color = if (state.remainingSeconds <= LOW_TIME_SECONDS) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { confirmExit = true }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.action_close))
                        }
                    },
                    actions = {
                        ReportIssueAction(state.question)
                        TextButton(onClick = { confirmFinish = true }, enabled = state.question != null) {
                            Text(stringResource(R.string.session_finish))
                        }
                    },
                )
                if (state.size > 0) {
                    QuestionStrip(state.size, state.index, state.answered, viewModel::goTo)
                }
            }
        },
        bottomBar = {
            if (state.question != null) {
                Row(
                    modifier = Modifier.navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = viewModel::previous, enabled = state.index > 0, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.mock_previous))
                    }
                    if (state.index + 1 < state.size) {
                        Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.session_next))
                        }
                    } else {
                        Button(onClick = { confirmFinish = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.session_finish))
                        }
                    }
                }
            }
        },
    ) { padding ->
        val question = state.question
        if (question == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
        } else {
            val scroll = rememberScrollState()
            LaunchedEffect(state.index) { scroll.scrollTo(0) }
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuestionBody(question)
                question.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option.text,
                        letter = 'A' + index,
                        state = if (option.id in state.selected) OptionState.SELECTED else OptionState.IDLE,
                        multi = question.type == QuestionType.MULTI,
                        onClick = { viewModel.toggle(option.id) },
                    )
                }
            }
        }
    }
}

/** Question numbers: answered ones are filled, the current one has a thick border. */
@Composable
private fun QuestionStrip(size: Int, current: Int, answered: Set<Int>, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) { listState.animateScrollToItem(current) }
    val colors = MaterialTheme.colorScheme

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(size) { index ->
            val isCurrent = index == current
            Surface(
                onClick = { onSelect(index) },
                shape = CircleShape,
                color = if (index in answered) colors.primaryContainer else colors.surface,
                border = BorderStroke(
                    if (isCurrent) 2.dp else 1.dp,
                    if (isCurrent) colors.primary else colors.outline.copy(alpha = 0.4f),
                ),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text((index + 1).toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
