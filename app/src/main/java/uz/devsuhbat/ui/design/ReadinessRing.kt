package uz.devsuhbat.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RingColors(val ring: Color, val track: Color, val cookie: Color, val text: Color)

object RingDefaults {
    @Composable
    fun colors(): RingColors {
        val c = MaterialTheme.colorScheme
        return RingColors(ring = c.primary, track = c.surfaceContainer, cookie = c.primaryContainer, text = c.onSurface)
    }

    /** For a ring drawn on a `primary` surface, such as the Home hero card. */
    @Composable
    fun onPrimaryColors(): RingColors {
        val on = MaterialTheme.colorScheme.onPrimary
        return RingColors(ring = on, track = on.copy(alpha = 0.22f), cookie = on.copy(alpha = 0.12f), text = on)
    }
}

/** A progress ring over a slowly turning cookie; the ring draws itself in on first show. */
@Composable
fun ReadinessRing(
    fraction: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    colors: RingColors = RingDefaults.colors(),
    centerStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val still = LocalReducedMotion.current
    val target = fraction.coerceIn(0f, 1f)
    val sweep = remember { Animatable(if (still) target else 0f) }
    LaunchedEffect(target, still) {
        if (still) sweep.snapTo(target) else sweep.animateTo(target, DsMotion.spatialSlow())
    }
    val angle = if (still) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "cookie")
        val turning by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
            label = "angle",
        )
        turning
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val points = cookiePoints(this.size.width, this.size.height, lobes = 9, depth = 0.07f)
            val cookie = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            rotate(angle) { drawPath(cookie, colors.cookie) }

            val stroke = this.size.minDimension * 0.07f
            val diameter = this.size.minDimension * 0.66f
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(colors.track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (sweep.value > 0f) {
                drawArc(
                    colors.ring, -90f, 360f * sweep.value, useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(centerText, style = centerStyle, color = colors.text)
    }
}
