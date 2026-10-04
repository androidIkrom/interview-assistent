package uz.devsuhbat.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import uz.devsuhbat.engine.Progress
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.Readiness
import uz.devsuhbat.engine.SessionResult
import uz.devsuhbat.ui.Routes
import uz.devsuhbat.ui.common.InlineCodeText
import uz.devsuhbat.ui.common.QuestionBody
import uz.devsuhbat.ui.common.ReportIssueAction
import uz.devsuhbat.ui.common.titleRes
import uz.devsuhbat.ui.design.ButtonTone
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.design.ExpressiveButton
import uz.devsuhbat.ui.design.FeedbackSheet
import uz.devsuhbat.ui.design.FeedbackTone
import uz.devsuhbat.ui.design.HapticEvent
import uz.devsuhbat.ui.design.OptionCard
import uz.devsuhbat.ui.design.WavyProgress
import uz.devsuhbat.ui.design.rememberHaptics
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

/** Readiness of the chosen field and level, as Home shows it; null before a field is chosen. */
private suspend fun currentReadiness(container: AppContainer): Progress? {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return null
    val level = settings.level ?: return null
    val states = container.progress.states.first()
    return withContext(container.io) {
        val content = container.content
        val scope = QuestionPicker.eligible(content.topicsOf(fieldId).flatMap { content.questions(it.id) }, level)
        Readiness.of(scope, states)
    }
}

private suspend fun logSession(container: AppContainer, topicId: String, startedAt: Instant, result: SessionResult) {
    val settings = container.settings.settings.first()
    val fieldId = settings.fieldId ?: return
    val level = settings.level ?: return
    val mode = if (topicId == Routes.MISTAKES) SessionMode.MISTAKES else SessionMode.PRACTICE
    container.progress.logSession(mode, fieldId, level, startedAt, result)
}

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
                    readiness = { currentReadiness(container) },
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
            readiness = readinessLine(state.readinessBefore, state.readinessAfter),
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

    val haptics = rememberHaptics()
    // One buzz per new verdict: a confirm for a right answer, a reject for a wrong one.
    LaunchedEffect(state.feedback) {
        when (state.feedback) {
            is Feedback.Correct -> haptics(HapticEvent.CORRECT)
            is Feedback.Wrong -> haptics(HapticEvent.WRONG)
            null -> Unit
        }
    }

    Scaffold(
        topBar = { SessionTopBar(state, onClose = { confirmExit = true }) },
        bottomBar = {
            if (state.question != null) {
                Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                    if (state.solved) {
                        val last = state.position + 1 == state.queueSize
                        ExpressiveButton(
                            text = stringResource(if (last) R.string.session_finish else R.string.session_next),
                            onClick = viewModel::next,
                            tone = ButtonTone.SUCCESS,
                        )
                    } else {
                        ExpressiveButton(
                            text = stringResource(R.string.session_check),
                            onClick = viewModel::check,
                            enabled = state.canCheck,
                        )
                    }
                }
            }
        },
    ) { padding ->
        val question = state.question
        if (question == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Each pane draws the state it was given, so the answered question slides out as it was left.
            AnimatedContent(
                targetState = state,
                contentKey = { Triple(it.question?.id, it.isRepeat, it.position) },
                transitionSpec = {
                    (slideInHorizontally(DsMotion.spatialDefault()) { it / 4 } + fadeIn(DsMotion.effectsDefault()))
                        .togetherWith(slideOutHorizontally(DsMotion.spatialDefault()) { -it / 4 } + fadeOut(DsMotion.effectsDefault()))
                },
                label = "question",
                modifier = Modifier.padding(padding),
            ) { pane ->
                val paneQuestion = pane.question ?: return@AnimatedContent
                QuestionContent(pane, paneQuestion) { optionId ->
                    haptics(HapticEvent.SELECT)
                    viewModel.toggle(optionId)
                }
            }
        }
    }
}

@Composable
private fun SessionTopBar(state: SessionUiState, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
        }
        val fraction = if (state.queueSize > 0) state.position.toFloat() / state.queueSize else 0f
        WavyProgress(fraction, Modifier.weight(1f))
        if (state.queueSize > 0) {
            Text("${state.position + 1} / ${state.queueSize}", style = MaterialTheme.typography.titleSmall)
        }
        ReportIssueAction(state.question)
    }
}

@Composable
private fun QuestionContent(state: SessionUiState, question: Question, onToggle: (String) -> Unit) {
    val scroll = rememberScrollState()
    // New feedback is brought into view at the bottom, just above the button.
    LaunchedEffect(state.feedback) { if (state.feedback != null) scroll.animateScrollTo(scroll.maxValue) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QuestionChips(state, question)
        QuestionBody(question)
        question.options.forEachIndexed { index, option ->
            OptionCard(
                text = option.text,
                letter = 'A' + index,
                state = optionState(option.id, state.selected, state.disabled, state.solved),
                multi = question.type == QuestionType.MULTI,
                onClick = { onToggle(option.id) },
            )
        }
        state.feedback?.let { SessionFeedback(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionChips(state: SessionUiState, question: Question) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(question.kind.titleRes), colors.primaryContainer, colors.onPrimaryContainer)
        Chip(stringResource(question.level.titleRes), colors.surfaceContainerLowest, colors.onSurface, BorderStroke(1.dp, colors.outlineVariant))
        if (state.isRepeat) {
            Chip(stringResource(R.string.session_repeat), extra.streakContainer, extra.onStreakContainer)
        }
    }
}

@Composable
private fun Chip(text: String, container: Color, content: Color, border: BorderStroke? = null) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(10.dp), border = border) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun SessionFeedback(feedback: Feedback) {
    when (feedback) {
        is Feedback.Correct -> FeedbackSheet(FeedbackTone.CORRECT, stringResource(R.string.session_correct_title)) {
            InlineCodeText(feedback.explanation, style = MaterialTheme.typography.bodyMedium)
        }
        is Feedback.Wrong -> FeedbackSheet(FeedbackTone.WRONG, stringResource(R.string.session_wrong_title)) {
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
