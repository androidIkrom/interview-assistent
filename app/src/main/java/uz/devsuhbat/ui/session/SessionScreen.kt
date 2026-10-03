package uz.devsuhbat.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.data.SessionMode
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.SessionResult
import uz.devsuhbat.ui.Routes
import uz.devsuhbat.ui.common.InlineCodeText
import uz.devsuhbat.ui.common.OptionCard
import uz.devsuhbat.ui.common.QuestionBody
import uz.devsuhbat.ui.theme.LocalExtraColors

/**
 * Picks the questions of a session. [topicId] is a topic id, [Routes.MIXED] for the whole field,
 * or [Routes.MISTAKES] for the questions that are due for repetition.
 */
private suspend fun loadSessionQuestions(container: AppContainer, topicId: String, random: Random): List<Question> {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return emptyList()
    val level = settings.level ?: return emptyList()
    val states = container.progress.states.first()
    val today = container.progress.today()
    return withContext(container.io) {
        val content = container.content
        fun fieldPool() = content.topicsOf(fieldId).flatMap { content.questions(it.id) }
        when (topicId) {
            Routes.MISTAKES -> QuestionPicker.mistakes(fieldPool(), level, states, today, random)
            Routes.MIXED -> QuestionPicker.practice(fieldPool(), level, random, states = states, today = today)
            else -> QuestionPicker.practice(content.questions(topicId), level, random, states = states, today = today)
        }
    }
}

private suspend fun logSession(container: AppContainer, topicId: String, startedAt: Instant, result: SessionResult) {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return
    val level = settings.level ?: return
    val mode = if (topicId == Routes.MISTAKES) SessionMode.MISTAKES else SessionMode.PRACTICE
    container.progress.logSession(mode, fieldId, level, startedAt, result)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    container: AppContainer,
    topicId: String,
    onExit: () -> Unit,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    val viewModel: SessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val random = Random.Default
                val startedAt = Instant.now()
                SessionViewModel(
                    loadQuestions = { loadSessionQuestions(container, topicId, random) },
                    random = random,
                    onOutcome = container.progress::record,
                    onFinished = { result -> logSession(container, topicId, startedAt, result) },
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result

    if (result != null) {
        BackHandler(onBack = onAgain)
        SessionResultScreen(
            result = result,
            onAgain = onAgain.takeIf { topicId != Routes.MISTAKES },
            onHome = onHome,
        )
        return
    }

    var confirmExit by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmExit = true }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.session_exit_title)) },
            text = { Text(stringResource(R.string.session_exit_text)) },
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (state.queueSize > 0) {
                            Text("${state.position + 1} / ${state.queueSize}", style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { confirmExit = true }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.action_close))
                        }
                    },
                )
                if (state.queueSize > 0) {
                    LinearProgressIndicator(
                        progress = { state.position.toFloat() / state.queueSize },
                        modifier = Modifier.fillMaxWidth(),
                        drawStopIndicator = {},
                    )
                }
            }
        },
        bottomBar = {
            if (state.question != null) {
                Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                    if (state.solved) {
                        val last = state.position + 1 == state.queueSize
                        Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (last) R.string.session_finish else R.string.session_next))
                        }
                    } else {
                        Button(onClick = viewModel::check, enabled = state.canCheck, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.session_check))
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
            QuestionContent(state, question, viewModel::toggle, Modifier.padding(padding))
        }
    }
}

@Composable
private fun QuestionContent(
    state: SessionUiState,
    question: Question,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    // A new question starts at the top; new feedback is brought into view at the bottom.
    LaunchedEffect(question.id, state.position) { scroll.scrollTo(0) }
    LaunchedEffect(state.feedback) { if (state.feedback != null) scroll.animateScrollTo(scroll.maxValue) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isRepeat) {
            AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.session_repeat)) })
        }
        QuestionBody(question)
        question.options.forEach { option ->
            OptionCard(
                option = option,
                multi = question.type == QuestionType.MULTI,
                selected = option.id in state.selected,
                onClick = { onToggle(option.id) },
                eliminated = option.id in state.disabled,
                solved = state.solved,
            )
        }
        state.feedback?.let { FeedbackPanel(it) }
    }
}

@Composable
private fun FeedbackPanel(feedback: Feedback) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    when (feedback) {
        is Feedback.Correct -> Surface(
            color = extra.successContainer,
            contentColor = extra.onSuccessContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.session_correct_title), style = MaterialTheme.typography.titleMedium)
                InlineCodeText(feedback.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
        is Feedback.Wrong -> Surface(
            color = colors.errorContainer,
            contentColor = colors.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.session_wrong_title), style = MaterialTheme.typography.titleMedium)
                if (feedback.correctPicked != null && feedback.correctMissing != null) {
                    Text(
                        text = stringResource(R.string.session_multi_feedback, feedback.correctPicked, feedback.correctMissing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                feedback.hints.forEach { (optionText, hint) ->
                    Column {
                        InlineCodeText(
                            text = optionText,
                            style = MaterialTheme.typography.labelLarge,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        InlineCodeText(hint, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
