package uz.devsuhbat.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.data.MockSummary
import uz.devsuhbat.engine.ActivityGrid
import uz.devsuhbat.engine.Breakdown
import uz.devsuhbat.ui.common.titleRes
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.design.ExpressiveButton
import uz.devsuhbat.ui.design.LocalReducedMotion
import uz.devsuhbat.ui.design.SectionCard
import uz.devsuhbat.ui.design.StatTile
import uz.devsuhbat.ui.home.TopicProgressRow
import uz.devsuhbat.ui.theme.LocalExtraColors

@Composable
fun StatsScreen(container: AppContainer, onTopic: (String) -> Unit, onMock: () -> Unit) {
    val viewModel: StatsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                StatsViewModel(
                    content = container.content,
                    settings = container.settings.settings,
                    states = container.progress.states,
                    sessionDays = container.progress.sessionDays(),
                    today = container.progress::today,
                    io = container.io,
                    mockHistory = { fieldId -> container.progress.mockHistory(fieldId) },
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
                val level = state.level?.let { stringResource(it.titleRes) }
                Text(
                    listOfNotNull(state.fieldTitle, level).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.loading) return@Column
            ActivityCard(state)
            LeitnerCard(state.breakdown)
            if (state.topics.isNotEmpty()) {
                SectionCard {
                    Text(stringResource(R.string.stats_topics), style = MaterialTheme.typography.titleMedium)
                    state.topics.forEach { topic -> TopicProgressRow(topic) { onTopic(topic.topicId) } }
                }
            }
            MockCard(state.mocks, state.mockAvailable, onMock)
        }
    }
}

@Composable
private fun ActivityCard(state: StatsUiState) {
    val extra = LocalExtraColors.current
    SectionCard {
        Text(stringResource(R.string.stats_activity), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                stringResource(R.string.home_streak_days, state.streak.current), stringResource(R.string.stats_streak_current),
                Modifier.weight(1f), container = extra.streakContainer, content = extra.onStreakContainer,
            )
            StatTile(
                stringResource(R.string.home_streak_days, state.streak.longest), stringResource(R.string.stats_streak_longest),
                Modifier.weight(1f), container = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
        Heatmap(state.grid)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.stats_weeks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.stats_less), style = MaterialTheme.typography.bodySmall)
            (0..3).forEach { level ->
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(heatColor(level)))
            }
            Text(stringResource(R.string.stats_more), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun heatColor(level: Int): Color {
    val c = MaterialTheme.colorScheme
    return when (level) {
        0 -> c.surfaceContainer
        1 -> c.primary.copy(alpha = 0.35f)
        2 -> c.primary.copy(alpha = 0.65f)
        else -> c.primary
    }
}

/** Weeks as columns, Monday at the top; days after today are left out and today has an outline. */
@Composable
private fun Heatmap(grid: List<List<Int?>>) {
    if (grid.isEmpty()) return
    val colors = (0..3).map { heatColor(it) }
    val outline = MaterialTheme.colorScheme.onSurface
    val activeDays = grid.flatten().count { (it ?: 0) > 0 }
    val description = stringResource(R.string.stats_heatmap_a11y, activeDays)
    val todayIndex = grid.last().indexOfLast { it != null }
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        val gap = 3.dp
        val cell = (maxWidth - gap * (grid.size - 1)) / grid.size
        Canvas(Modifier.fillMaxWidth().height(cell * 7 + gap * 6)) {
            val cellPx = cell.toPx()
            val gapPx = gap.toPx()
            val radius = CornerRadius(3.dp.toPx())
            grid.forEachIndexed { week, days ->
                days.forEachIndexed { weekday, count ->
                    if (count == null) return@forEachIndexed
                    val topLeft = Offset(week * (cellPx + gapPx), weekday * (cellPx + gapPx))
                    drawRoundRect(colors[ActivityGrid.level(count)], topLeft, Size(cellPx, cellPx), radius)
                    if (week == grid.lastIndex && weekday == todayIndex) {
                        drawRoundRect(outline, topLeft, Size(cellPx, cellPx), radius, style = Stroke(1.5.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
private fun LeitnerCard(breakdown: Breakdown) {
    val c = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val parts = listOf(
        Triple(R.string.stats_fresh, breakdown.fresh, c.surfaceContainer),
        Triple(R.string.stats_learning, breakdown.learning, extra.streak),
        Triple(R.string.stats_mastered, breakdown.mastered, extra.success),
    )
    SectionCard {
        Text(stringResource(R.string.stats_leitner), style = MaterialTheme.typography.titleMedium)
        if (breakdown.total == 0) {
            Text(stringResource(R.string.topics_no_content), style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
            parts.filter { it.second > 0 }.forEach { (_, count, color) ->
                Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(color))
            }
        }
        parts.forEach { (label, count, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(count.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MockCard(mocks: List<MockSummary>, available: Boolean, onMock: () -> Unit) {
    SectionCard {
        Text(stringResource(R.string.stats_mock), style = MaterialTheme.typography.titleMedium)
        if (mocks.isEmpty()) {
            Text(
                stringResource(R.string.stats_mock_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExpressiveButton(stringResource(R.string.stats_mock_start), onClick = onMock, enabled = available)
        } else {
            MockChart(mocks)
        }
    }
}

private fun MockSummary.percent(): Int = if (total == 0) 0 else correct * 100 / total

/** One bar per mock, oldest on the left; the newest is drawn in the success colour. */
@Composable
private fun MockChart(mocks: List<MockSummary>) {
    val c = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = c.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val still = LocalReducedMotion.current
    val grow = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) grow.animateTo(1f, DsMotion.spatialDefault()) }
    val description = stringResource(R.string.stats_mock_a11y, mocks.size, mocks.last().percent())

    Canvas(Modifier.fillMaxWidth().height(160.dp).clearAndSetSemantics { contentDescription = description }) {
        val gap = 8.dp.toPx()
        val labelRoom = 18.dp.toPx()
        val barWidth = minOf(28.dp.toPx(), (size.width - gap * (mocks.size - 1)) / mocks.size)
        val chartHeight = size.height - labelRoom
        mocks.forEachIndexed { index, mock ->
            val percent = mock.percent()
            val barHeight = chartHeight * percent / 100f * grow.value
            val left = index * (barWidth + gap)
            val top = size.height - barHeight
            drawRoundRect(
                color = if (index == mocks.lastIndex) extra.success else c.primary,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            val label = measurer.measure("$percent%", labelStyle)
            drawText(label, topLeft = Offset(left + (barWidth - label.size.width) / 2f, top - label.size.height - 2.dp.toPx()))
        }
    }
}
