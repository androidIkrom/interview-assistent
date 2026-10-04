package uz.devsuhbat.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfettiTest {
    @Test
    fun createsTheRequestedCount() {
        assertEquals(40, confettiParticles().size)
    }

    @Test
    fun valuesStayInRange() {
        confettiParticles().forEach {
            assertTrue("$it", it.xFraction in 0f..1f)
            assertTrue("$it", it.widthDp in 6f..12f)
            assertTrue("$it", it.heightDp in 10f..18f)
            assertTrue("$it", it.colorIndex in 0..4)
            assertTrue("$it", it.spinDegrees in -360f..360f)
            assertTrue("$it", it.durationMillis in 2200..3600)
            assertTrue("$it", it.delayMillis in 500..1200)
        }
    }

    @Test
    fun sameSeedSameParticles() {
        assertEquals(confettiParticles(seed = 7), confettiParticles(seed = 7))
        assertNotEquals(confettiParticles(seed = 7), confettiParticles(seed = 8))
    }

    @Test
    fun confettiShowsOnlyWhenPlayingAndMotionAllowed() {
        assertTrue(confettiVisible(play = true, reducedMotion = false))
        assertFalse(confettiVisible(play = true, reducedMotion = true))
        assertFalse(confettiVisible(play = false, reducedMotion = false))
        assertFalse(confettiVisible(play = false, reducedMotion = true))
    }
}
