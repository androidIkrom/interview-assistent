package uz.devsuhbat.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.data.MockSummary
import uz.devsuhbat.engine.DayMark
import uz.devsuhbat.engine.NextReview
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.engine.StreakInfo
import uz.devsuhbat.ui.common.titleRes
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.design.ExpressiveButton
import uz.devsuhbat.ui.design.LocalReducedMotion
import uz.devsuhbat.ui.design.ReadinessRing
import uz.devsuhbat.ui.design.RingColors
import uz.devsuhbat.ui.design.SectionCard
import uz.devsuhbat.ui.theme.LocalExtraColors

@Composable
fun HomeScreen(
    container: AppContainer,
    onPractice: () -> Unit,
    onMistakes: () -> Unit,
    onMock: () -> Unit,
    onTopic: (topicId: String) -> Unit,
    onAllTopics: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    content = container.content,
                    settings = container.settings.settings,
                    states = container.progress.states,
                    today = container.progress::today,
                    io = container.io,
                    sessionDays = container.progress.sessionDays(),
                    lastMock = container.progress::lastMock,
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(onSettings)
            if (state.loading) return@Column
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Rise(0) { HeroCard(state, onPractice) }
                Rise(80) { Tiles(state, onMistakes) }
                Rise(200) { MockCard(state.mockQuestionCount, state.lastMock, onMock) }
                if (state.weakTopics.isNotEmpty()) {
                    Rise(260) { WeakTopicsCard(state.weakTopics, onTopic, onAllTopics) }
                }
                Box(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.settings_title))
        }
    }
}

/** Rises into place once when the screen is first shown; still under reduced motion. */
@Composable
private fun Rise(delayMillis: Int, content: @Composable () -> Unit) {
    val still = LocalReducedMotion.current
    val distance = with(LocalDensity.current) { 24.dp.toPx() }
    val offset = remember { Animatable(if (still) 0f else distance) }
    val alpha = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (still) return@LaunchedEffect
        delay(delayMillis.toLong())
        launch { offset.animateTo(0f, DsMotion.spatialDefault()) }
        alpha.animateTo(1f, DsMotion.effectsDefault())
    }
    Box(Modifier.graphicsLayer { translationY = offset.value; this.alpha = alpha.value }) { content() }
}

@Composable
private fun HeroCard(state: HomeUiState, onPractice: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A filled primary card in the light theme; the softer container in the dark theme, where primary is pale.
    val light = colors.primary.luminance() < 0.5f
    val container = if (light) colors.primary else colors.primaryContainer
    val content = if (light) colors.onPrimary else colors.onPrimaryContainer
    val percent = state.readiness.percent

    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.home_your_target), style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.fieldTitle.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, fill = false))
                    state.level?.let { level ->
                        Text(
                            stringResource(level.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(content.copy(alpha = 0.18f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (state.readiness.total > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReadinessRing(
                        fraction = state.readiness.mastered.toFloat() / state.readiness.total,
                        centerText = stringResource(R.string.home_percent, percent),
                        size = 120.dp,
                        colors = RingColors(ring = content, track = content.copy(alpha = 0.22f), cookie = content.copy(alpha = 0.12f), text = content),
                        contentDescription = stringResource(R.string.home_readiness, percent),
                    )
                    Column {
                        Text(
                            "${state.readiness.mastered} / ${state.readiness.total}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(stringResource(R.string.home_mastered_label), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            ExpressiveButton(
                text = stringResource(R.string.home_start_practice),
                onClick = onPractice,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                containerColor = content,
                contentColor = container,
            )
        }
    }
}

@Composable
private fun Tiles(state: HomeUiState, onMistakes: () -> Unit) {
    // At very large text the two tiles would squeeze their content, so they stack.
    if (LocalDensity.current.fontScale > 1.5f) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MistakesTile(state.dueCount, state.nextReview, onMistakes, Modifier.fillMaxWidth())
            StreakTile(state.streak, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            MistakesTile(state.dueCount, state.nextReview, onMistakes, Modifier.weight(1f).fillMaxHeight())
            StreakTile(state.streak, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun Tile(container: Color, content: Color, modifier: Modifier, body: @Composable ColumnScope.() -> Unit) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = body)
    }
}

@Composable
private fun MistakesTile(dueCount: Int, nextReview: NextReview?, onMistakes: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Tile(colors.errorContainer, colors.onErrorContainer, modifier) {
        Text(dueCount.toString(), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.home_mistakes_tile), style = MaterialTheme.typography.bodyMedium)
        if (dueCount > 0) {
            Box(Modifier.weight(1f, fill = false))
            Surface(
                onClick = onMistakes,
                color = colors.error,
                contentColor = colors.onError,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Text(
                    stringResource(R.string.home_mistakes_action),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        } else {
            Text(
                text = when {
                    nextReview == null -> stringResource(R.string.home_mistakes_none)
                    nextReview.inDays == 1 -> stringResource(R.string.home_mistakes_tomorrow, nextReview.count)
                    else -> stringResource(R.string.home_mistakes_later, nextReview.count, nextReview.inDays)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StreakTile(streak: StreakInfo, modifier: Modifier) {
    val extra = LocalExtraColors.current
    val a11y = stringResource(R.string.home_streak_a11y, streak.current)
    Tile(extra.streakContainer, extra.onStreakContainer, modifier.semantics(mergeDescendants = true) { contentDescription = a11y }) {
        if (streak.current > 0) {
            Text(stringResource(R.string.home_streak_days, streak.current), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.home_streak_label), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.home_streak_start), style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            streak.week.forEach { mark -> DayDot(mark) }
        }
    }
}

@Composable
private fun DayDot(mark: DayMark) {
    val extra = LocalExtraColors.current
    val dot = Modifier.size(16.dp).clip(CircleShape)
    when (mark) {
        DayMark.DONE -> Box(dot.background(extra.streak))
        DayMark.MISSED -> Box(dot.background(extra.onStreakContainer.copy(alpha = 0.12f)))
        DayMark.FUTURE -> Box(dot.background(extra.onStreakContainer.copy(alpha = 0.06f)))
        DayMark.TODAY_PENDING -> {
            val scale = if (LocalReducedMotion.current) {
                1f
            } else {
                val pulse by rememberInfiniteTransition(label = "today").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "pulse",
                )
                pulse
            }
            Box(dot.graphicsLayer { scaleX = scale; scaleY = scale }.border(2.dp, extra.streak, CircleShape))
        }
    }
}

@Composable
private fun MockCard(questionCount: Int, lastMock: MockSummary?, onMock: () -> Unit) {
    val available = questionCount >= QuestionPicker.MOCK_MIN
    val colors = MaterialTheme.colorScheme
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_mock), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (available) stringResource(R.string.home_mock_meta, questionCount) else stringResource(R.string.home_mock_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                if (lastMock != null) {
                    Text(
                        stringResource(R.string.home_mock_last, lastMock.correct, lastMock.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            FilledIconButton(onClick = onMock, enabled = available, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start))
            }
        }
    }
}

@Composable
private fun WeakTopicsCard(topics: List<TopicProgress>, onTopic: (String) -> Unit, onAllTopics: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_weak_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onAllTopics) { Text(stringResource(R.string.home_weak_all)) }
        }
        topics.forEach { topic -> WeakTopicRow(topic) { onTopic(topic.topicId) } }
    }
}

@Composable
private fun WeakTopicRow(topic: TopicProgress, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val target = topic.progress.percent / 100f
    val still = LocalReducedMotion.current
    val fill = remember { Animatable(if (still) target else 0f) }
    LaunchedEffect(target) { if (still) fill.snapTo(target) else fill.animateTo(target, DsMotion.spatialDefault()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Row {
            Text(topic.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.home_percent, topic.progress.percent),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceContainer)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value).clip(RoundedCornerShape(4.dp)).background(colors.primary))
        }
    }
}
