package uz.devsuhbat.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.ui.Routes
import uz.devsuhbat.ui.common.CodeBlock
import uz.devsuhbat.ui.common.InlineCodeText
import uz.devsuhbat.ui.theme.LocalExtraColors

/** Picks the practice questions for [topicId] (or for the whole field when it is [Routes.MIXED]). */
private suspend fun loadPracticeQuestions(container: AppContainer, topicId: String, random: Random): List<Question> {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return emptyList()
    val level = settings.level ?: return emptyList()
    return withContext(container.io) {
        val content = container.content
        val pool = if (topicId == Routes.MIXED) {
            content.topicsOf(fieldId).flatMap { content.questions(it.id) }
        } else {
            content.questions(topicId)
        }
        QuestionPicker.practice(pool, level, random)
    }
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
                SessionViewModel({ loadPracticeQuestions(container, topicId, random) }, random)
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result

    if (result != null) {
        BackHandler(onBack = onAgain)
        SessionResultScreen(result = result, onAgain = onAgain, onHome = onHome)
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
        InlineCodeText(question.prompt, style = MaterialTheme.typography.titleMedium)
        question.code?.let { CodeBlock(it) }
        if (question.type == QuestionType.MULTI) {
            Text(
                text = stringResource(R.string.session_multi_caption),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        question.options.forEach { option ->
            OptionCard(
                option = option,
                multi = question.type == QuestionType.MULTI,
                selected = option.id in state.selected,
                eliminated = option.id in state.disabled,
                solved = state.solved,
                onClick = { onToggle(option.id) },
            )
        }
        state.feedback?.let { FeedbackPanel(it) }
    }
}

/**
 * Before the question is solved an option only shows "selected" or "eliminated"; nothing marks a correct one.
 * Once solved, the selected options are the correct set and are drawn in the success colour.
 */
@Composable
private fun OptionCard(
    option: Option,
    multi: Boolean,
    selected: Boolean,
    eliminated: Boolean,
    solved: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val confirmed = solved && selected
    val container = when {
        confirmed -> extra.successContainer
        selected -> colors.primaryContainer
        else -> colors.surface
    }
    val border = when {
        confirmed -> extra.success
        selected -> colors.primary
        else -> colors.outline.copy(alpha = 0.4f)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (eliminated) 0.45f else 1f)
            .selectable(
                selected = selected,
                enabled = !eliminated && !solved,
                role = if (multi) Role.Checkbox else Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multi) {
                Checkbox(checked = selected, onCheckedChange = null, enabled = !eliminated)
            } else {
                RadioButton(selected = selected, onClick = null, enabled = !eliminated)
            }
            InlineCodeText(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (eliminated) TextDecoration.LineThrough else null,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
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
