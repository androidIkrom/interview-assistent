package uz.devsuhbat.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import uz.devsuhbat.ui.theme.LocalExtraColors
import kotlin.random.Random

data class ConfettiParticle(
    val xFraction: Float,
    val widthDp: Float,
    val heightDp: Float,
    val colorIndex: Int,
    val spinDegrees: Float,
    val durationMillis: Int,
    val delayMillis: Int,
)

fun confettiParticles(count: Int = 40, seed: Int = 7): List<ConfettiParticle> {
    val random = Random(seed)
    fun between(from: Float, to: Float) = from + random.nextFloat() * (to - from)
    return List(count) { i ->
        ConfettiParticle(
            xFraction = random.nextFloat(),
            widthDp = between(6f, 12f),
            heightDp = between(10f, 18f),
            colorIndex = i % 5,
            spinDegrees = between(-360f, 360f),
            durationMillis = random.nextInt(2200, 3601),
            delayMillis = random.nextInt(500, 1201),
        )
    }
}

fun confettiVisible(play: Boolean, reducedMotion: Boolean): Boolean = play && !reducedMotion

private const val FADE_FROM = 0.7f

/** A one-off burst of falling pieces over the whole [modifier] area, started when [play] turns true. */
@Composable
fun Confetti(play: Boolean, modifier: Modifier = Modifier) {
    if (!confettiVisible(play, LocalReducedMotion.current)) return
    val c = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val palette = listOf(c.primary, extra.streak, extra.success, c.primaryContainer, Color(0xFFFFC83D))
    val particles = remember { confettiParticles() }
    val totalMillis = remember(particles) { particles.maxOf { it.delayMillis + it.durationMillis } }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(totalMillis.toFloat(), tween(totalMillis, easing = LinearEasing)) }

    Canvas(modifier) {
        val start = -24.dp.toPx()
        val travel = size.height - start + 48.dp.toPx()
        particles.forEach { p ->
            val t = (clock.value - p.delayMillis) / p.durationMillis
            if (t <= 0f || t >= 1f) return@forEach
            val w = p.widthDp.dp.toPx()
            val h = p.heightDp.dp.toPx()
            val center = Offset(p.xFraction * size.width, start + travel * t)
            val alpha = if (t < FADE_FROM) 1f else 1f - 0.8f * (t - FADE_FROM) / (1f - FADE_FROM)
            rotate(p.spinDegrees * t, pivot = center) {
                drawRoundRect(
                    color = palette[p.colorIndex].copy(alpha = alpha),
                    topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}
