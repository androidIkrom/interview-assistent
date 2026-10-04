package uz.devsuhbat.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.devsuhbat.R
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.design.LocalReducedMotion

/** A topic's title, mastered percent and a bar that fills in once; used on Home and on Statistics. */
@Composable
internal fun TopicProgressRow(topic: TopicProgress, onClick: () -> Unit) {
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
