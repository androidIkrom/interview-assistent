package uz.devsuhbat.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import uz.devsuhbat.ui.common.InlineCodeText
import uz.devsuhbat.ui.theme.ExtraColors
import uz.devsuhbat.ui.theme.LocalExtraColors

enum class OptionState {
    IDLE, SELECTED, WRONG, CORRECT, DIMMED;

    /** Only an option that is still in play can be tapped. */
    val interactive: Boolean get() = this == IDLE || this == SELECTED
}

/** A wrong option shakes once, when it turns wrong; never on first show, e.g. when scrolled back into view. */
fun shouldShake(previous: OptionState?, current: OptionState): Boolean =
    current == OptionState.WRONG && previous != null && previous != OptionState.WRONG

internal class OptionPalette(
    val container: Color,
    val content: Color,
    val border: Color,
    val badge: Color,
    val onBadge: Color,
)

internal fun optionPalette(state: OptionState, c: ColorScheme, extra: ExtraColors): OptionPalette = when (state) {
    OptionState.IDLE, OptionState.DIMMED ->
        OptionPalette(c.surfaceContainerLowest, c.onSurface, c.outlineVariant, c.surfaceContainer, c.primary)
    OptionState.SELECTED -> OptionPalette(c.primaryContainer, c.onPrimaryContainer, c.primary, c.primary, c.onPrimary)
    OptionState.WRONG -> OptionPalette(
        c.errorContainer.copy(alpha = 0.35f).compositeOver(c.surfaceContainerLowest),
        c.onSurfaceVariant,
        c.error.copy(alpha = 0.35f),
        c.errorContainer,
        c.error,
    )
    OptionState.CORRECT ->
        OptionPalette(extra.successContainer, extra.onSuccessContainer, extra.success, extra.success, extra.onSuccess)
}

/**
 * Plays the shake into WRONG or the pop into CORRECT. When the state changes again mid-way this is cancelled,
 * and the card must come back to rest rather than stay shifted or enlarged.
 */
internal suspend fun playOptionMotion(
    previous: OptionState?,
    state: OptionState,
    shake: Animatable<Float, AnimationVector1D>,
    pop: Animatable<Float, AnimationVector1D>,
    shakePx: Float,
) {
    try {
        if (shouldShake(previous, state)) {
            shake.animateTo(0f, keyframes {
                durationMillis = 550
                -shakePx at 80
                shakePx at 160
                -shakePx at 240
                shakePx at 320
                -shakePx * 0.45f at 420
            })
        }
        if (state == OptionState.CORRECT && previous != null && previous != OptionState.CORRECT) {
            pop.animateTo(1.045f, DsMotion.spatialFast())
            pop.animateTo(1f, DsMotion.spatialFast())
        }
    } finally {
        withContext(NonCancellable) {
            shake.snapTo(0f)
            pop.snapTo(1f)
        }
    }
}

/** One answer option: its corners and badge morph with the state, a wrong pick shakes, a right one pops. */
@Composable
fun OptionCard(
    text: String,
    letter: Char,
    state: OptionState,
    multi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val still = LocalReducedMotion.current
    val target = optionPalette(state, MaterialTheme.colorScheme, LocalExtraColors.current)
    val container by animateColorAsState(target.container, DsMotion.effectsDefault(), label = "container")
    val content by animateColorAsState(target.content, DsMotion.effectsDefault(), label = "content")
    val border by animateColorAsState(target.border, DsMotion.effectsDefault(), label = "border")
    val badge by animateColorAsState(target.badge, DsMotion.effectsDefault(), label = "badge")
    val corner by animateDpAsState(
        when (state) {
            OptionState.SELECTED -> 32.dp
            OptionState.CORRECT -> 34.dp
            else -> 22.dp
        },
        DsMotion.spatialDefault(),
        label = "corner",
    )
    val rounded = state == OptionState.SELECTED || state == OptionState.CORRECT
    val badgeCorner by animateDpAsState(
        if (!rounded) 13.dp else if (multi) 10.dp else 19.dp,
        DsMotion.spatialFast(),
        label = "badgeCorner",
    )

    val shakePx = with(LocalDensity.current) { 9.dp.toPx() }
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val last = remember { mutableStateOf<OptionState?>(null) }
    LaunchedEffect(state) {
        val previous = last.value
        last.value = state
        if (!still) playOptionMotion(previous, state, shake, pop, shakePx)
    }

    Surface(
        onClick = onClick,
        enabled = state.interactive,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        border = BorderStroke(1.5.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .graphicsLayer {
                translationX = shake.value
                scaleX = pop.value
                scaleY = pop.value
            }
            .alpha(if (state == OptionState.DIMMED) 0.45f else 1f)
            .semantics {
                role = if (multi) Role.Checkbox else Role.RadioButton
                selected = rounded
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(badgeCorner)).background(badge),
            ) {
                when (state) {
                    OptionState.WRONG -> Icon(Icons.Rounded.Close, null, tint = target.onBadge, modifier = Modifier.size(20.dp))
                    OptionState.CORRECT -> CheckMark(target.onBadge, still)
                    else -> Text(letter.toString(), style = MaterialTheme.typography.titleMedium, color = target.onBadge)
                }
            }
            InlineCodeText(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (state == OptionState.WRONG) TextDecoration.LineThrough else null,
            )
        }
    }
}

@Composable
private fun CheckMark(tint: Color, still: Boolean) {
    val scale = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) scale.animateTo(1f, DsMotion.spatialFast()) }
    Icon(
        Icons.Rounded.Check,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}
