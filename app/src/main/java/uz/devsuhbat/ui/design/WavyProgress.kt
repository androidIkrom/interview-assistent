package uz.devsuhbat.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/** Points of a sine wave across [width], [phase] shifting it along x. The last point sits exactly at [width]. */
fun wavePoints(width: Float, centerY: Float, amplitude: Float, wavelength: Float, phase: Float, step: Float = 2f): List<Offset> {
    val count = ceil(width / step).toInt()
    return List(count + 1) { i ->
        val x = if (i == count) width else i * step
        Offset(x, centerY + amplitude * sin(2.0 * PI * (x + phase) / wavelength).toFloat())
    }
}

/** Session progress: a moving wave up to [fraction], then a flat track. */
@Composable
fun WavyProgress(fraction: Float, modifier: Modifier = Modifier) {
    val active = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val still = LocalReducedMotion.current
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), DsMotion.spatialDefault(), label = "fraction")
    val phase = if (still) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "wave")
        val moving by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        moving
    }

    Canvas(modifier.fillMaxWidth().height(20.dp)) {
        val stroke = 5.dp.toPx()
        val wavelength = 16.dp.toPx()
        val gap = 10.dp.toPx()
        val end = size.width * shown
        val centerY = size.height / 2f

        if (end > 0f) {
            val points = wavePoints(size.width, centerY, 3.2.dp.toPx(), wavelength, phase * wavelength)
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            clipRect(right = end) { drawPath(path, active, style = Stroke(stroke, cap = StrokeCap.Round)) }
        }
        val trackStart = if (end > 0f) end + gap else 0f
        if (trackStart < size.width) {
            drawLine(track, Offset(trackStart, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
        }
    }
}
