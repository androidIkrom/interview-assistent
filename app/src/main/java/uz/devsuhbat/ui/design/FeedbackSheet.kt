package uz.devsuhbat.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import uz.devsuhbat.ui.theme.LocalExtraColors

enum class FeedbackTone { WRONG, CORRECT }

/** The panel under the options after a check: a hint for a wrong pick, the explanation for a right one. */
@Composable
fun FeedbackSheet(
    tone: FeedbackTone,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val still = LocalReducedMotion.current
    val visible = remember { MutableTransitionState(still).apply { targetState = true } }
    val (container, onContainer) = when (tone) {
        FeedbackTone.WRONG -> c.errorContainer to c.onErrorContainer
        FeedbackTone.CORRECT -> extra.successContainer to extra.onSuccessContainer
    }

    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(DsMotion.spatialDefault()) { it } + fadeIn(DsMotion.effectsDefault()),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(color = container, contentColor = onContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (tone) {
                        FeedbackTone.WRONG -> Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceContainerLowest),
                        ) { Icon(Icons.Rounded.Lightbulb, null, tint = c.error, modifier = Modifier.size(20.dp)) }
                        FeedbackTone.CORRECT -> Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).clip(CookieShape()).background(extra.success),
                        ) { Icon(Icons.Rounded.Check, null, tint = extra.onSuccess, modifier = Modifier.size(20.dp)) }
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                content()
            }
        }
    }
}
