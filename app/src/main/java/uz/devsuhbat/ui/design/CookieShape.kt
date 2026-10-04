package uz.devsuhbat.ui.design

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Outline of a scalloped "cookie": a circle whose radius waves [lobes] times around it, [depth] being the
 * wave's share of the radius. The outline is centred in [width] x [height] and its bumps touch the shorter side.
 */
fun cookiePoints(width: Float, height: Float, lobes: Int, depth: Float, steps: Int = 144): List<Offset> {
    val center = Offset(width / 2f, height / 2f)
    val base = min(width, height) / 2f / (1f + depth)
    return List(steps) { i ->
        val t = 2.0 * PI * i / steps
        val r = base * (1f + depth * cos(lobes * t).toFloat())
        center + Offset(r * cos(t).toFloat(), r * sin(t).toFloat())
    }
}

/** The Expressive "cookie" shape used behind rings and on the correct-answer mark. */
class CookieShape(private val lobes: Int = 9, private val depth: Float = 0.07f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = cookiePoints(size.width, size.height, lobes, depth)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }
}
