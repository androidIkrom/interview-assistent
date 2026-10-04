package uz.devsuhbat.ui.design

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieShapeTest {

    private fun distances(points: List<Offset>, center: Offset) = points.map { (it - center).getDistance() }

    @Test
    fun pointCountEqualsSteps() {
        assertEquals(144, cookiePoints(100f, 100f, 9, 0.07f).size)
    }

    @Test
    fun outerRadiusTouchesTheBoundsAndInnerIsDepthDeeper() {
        val d = distances(cookiePoints(100f, 100f, 9, 0.07f), Offset(50f, 50f))
        assertEquals(50f, d.max(), 0.5f)
        assertEquals(50f / 1.07f * 0.93f, d.min(), 0.5f)
    }

    @Test
    fun hasOneBumpPerLobe() {
        val d = distances(cookiePoints(100f, 100f, 9, 0.07f), Offset(50f, 50f))
        val maxima = d.indices.count { i ->
            val prev = d[(i - 1 + d.size) % d.size]
            val next = d[(i + 1) % d.size]
            d[i] > prev && d[i] >= next
        }
        assertEquals(9, maxima)
    }

    @Test
    fun cookieStaysInsideNonSquareBounds() {
        val points = cookiePoints(200f, 100f, 9, 0.07f)
        points.forEach {
            assertTrue("x out of bounds: $it", it.x in 0f..200f)
            assertTrue("y out of bounds: $it", it.y in 0f..100f)
        }
        val center = Offset(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
        assertEquals(100f, center.x, 0.5f)
        assertEquals(50f, center.y, 0.5f)
    }
}
