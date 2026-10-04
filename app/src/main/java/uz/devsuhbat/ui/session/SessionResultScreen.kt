package uz.devsuhbat.ui.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import uz.devsuhbat.R
import uz.devsuhbat.engine.ResultHeadline
import uz.devsuhbat.engine.SessionResult
import uz.devsuhbat.engine.celebrates
import uz.devsuhbat.ui.design.ButtonTone
import uz.devsuhbat.ui.design.Confetti
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.design.ExpressiveButton
import uz.devsuhbat.ui.design.LocalReducedMotion
import uz.devsuhbat.ui.design.ReadinessRing
import uz.devsuhbat.ui.design.SectionCard
import uz.devsuhbat.ui.design.StatTile
import uz.devsuhbat.ui.theme.LocalExtraColors

@Composable
fun SessionResultScreen(result: SessionResult, readiness: ReadinessLine?, onAgain: (() -> Unit)?, onHome: () -> Unit) {
    val headline = ResultHeadline.of(result.firstTryCorrect, result.total)
    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // A mistakes session has no topic list to go back to, so "Bosh sahifa" is its only action.
                if (onAgain != null) {
                    ExpressiveButton(stringResource(R.string.result_again), onClick = onAgain)
                    ExpressiveButton(stringResource(R.string.result_home), onClick = onHome, tone = ButtonTone.OUTLINED)
                } else {
                    ExpressiveButton(stringResource(R.string.result_home), onClick = onHome)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (result.total == 0) {
                Text(
                    stringResource(R.string.result_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .statusBarsPadding().padding(horizontal = 20.dp, vertical = 32.dp),
                ) {
                    Text(
                        stringResource(headline.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    ScoreRing(result)
                    StatTiles(result)
                    readiness?.let { ReadinessCard(it) }
                }
                Confetti(play = headline.celebrates, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private val ResultHeadline.titleRes: Int
    get() = when (this) {
        ResultHeadline.GREAT -> R.string.result_great
        ResultHeadline.GOOD -> R.string.result_good
        ResultHeadline.KEEP_GOING -> R.string.result_keep_going
    }

/** The score counts up from zero once; under reduced motion it shows the final value at once. */
@Composable
private fun ScoreRing(result: SessionResult) {
    val still = LocalReducedMotion.current
    val shown = remember { Animatable(if (still) result.firstTryCorrect.toFloat() else 0f) }
    LaunchedEffect(result.firstTryCorrect) {
        if (!still) shown.animateTo(result.firstTryCorrect.toFloat(), tween(900))
    }
    ReadinessRing(
        fraction = result.firstTryCorrect.toFloat() / result.total,
        centerText = stringResource(R.string.result_score, shown.value.roundToInt(), result.total),
        size = 200.dp,
        centerStyle = MaterialTheme.typography.displaySmall,
    )
}

@Composable
private fun StatTiles(result: SessionResult) {
    val extra = LocalExtraColors.current
    val total: @Composable (Modifier) -> Unit = {
        StatTile(result.total.toString(), stringResource(R.string.result_total), it, enterDelayMillis = 550)
    }
    val firstTry: @Composable (Modifier) -> Unit = {
        StatTile(
            result.firstTryCorrect.toString(), stringResource(R.string.result_first_try), it,
            container = extra.successContainer, content = extra.onSuccessContainer, enterDelayMillis = 650,
        )
    }
    val reworked: @Composable (Modifier) -> Unit = {
        StatTile(
            result.reworked.toString(), stringResource(R.string.result_reworked), it,
            container = extra.streakContainer, content = extra.onStreakContainer, enterDelayMillis = 750,
        )
    }
    if (LocalDensity.current.fontScale > 1.5f) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(total, firstTry, reworked).forEach { it(Modifier.fillMaxWidth()) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(total, firstTry, reworked).forEach { it(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ReadinessCard(line: ReadinessLine) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val value = when (line) {
        is ReadinessLine.Pending -> line.percent ?: return
        is ReadinessLine.Current -> line.percent
        is ReadinessLine.Grew -> null
    }
    SectionCard {
        if (value != null) {
            Text(stringResource(R.string.result_readiness_value, value), style = MaterialTheme.typography.titleMedium)
            return@SectionCard
        }
        val grew = line as ReadinessLine.Grew
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.result_readiness), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.result_readiness_grew, grew.from, grew.to),
                style = MaterialTheme.typography.titleMedium,
                color = extra.success,
            )
        }
        val still = LocalReducedMotion.current
        val growth = remember { Animatable(if (still) 1f else 0f) }
        LaunchedEffect(grew) { if (!still) growth.animateTo(1f, DsMotion.spatialDefault()) }
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.surfaceContainer)) {
            if (grew.from > 0) {
                Box(Modifier.weight(grew.from.toFloat()).fillMaxHeight().background(colors.primary))
            }
            Box(Modifier.weight((grew.to - grew.from).toFloat()).fillMaxHeight()) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(growth.value).background(extra.success))
            }
            if (grew.to < 100) {
                Box(Modifier.weight((100 - grew.to).toFloat()).fillMaxHeight())
            }
        }
    }
}
