package uz.devsuhbat.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavePointsTest {

    private fun yAt(x: Float, phase: Float) =
        wavePoints(width = 64f, centerY = 10f, amplitude = 3.2f, wavelength = 16f, phase = phase, step = 2f)
            .first { it.x == x }.y

    @Test
    fun startsAtZeroAndEndsAtWidth() {
        val points = wavePoints(width = 200f, centerY = 10f, amplitude = 3.2f, wavelength = 16f, phase = 0f, step = 2f)
        assertEquals(0f, points.first().x)
        assertEquals(200f, points.last().x)
    }

    @Test
    fun staysWithinAmplitude() {
        wavePoints(width = 200f, centerY = 10f, amplitude = 3.2f, wavelength = 16f, phase = 0f).forEach {
            assertTrue("y out of range: $it", it.y in (10f - 3.2f - 0.001f)..(10f + 3.2f + 0.001f))
        }
    }

    @Test
    fun repeatsEveryWavelength() {
        for (x in listOf(0f, 2f, 6f, 10f)) assertEquals(yAt(x, 0f), yAt(x + 16f, 0f), 0.01f)
    }

    @Test
    fun phaseShiftsTheWave() {
        assertEquals(yAt(4f, 0f), yAt(0f, 4f), 0.01f)
    }
}
