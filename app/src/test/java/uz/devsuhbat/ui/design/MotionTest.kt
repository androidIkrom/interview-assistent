package uz.devsuhbat.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun onlyAZeroAnimatorScaleMeansReducedMotion() {
        assertTrue(isReducedMotion(0f))
        assertFalse(isReducedMotion(1f))
        assertFalse(isReducedMotion(0.5f))
    }

    @Test
    fun springTokensUseTheSpecValues() {
        val fast = DsMotion.spatialFast<Float>()
        assertEquals(0.6f, fast.dampingRatio)
        assertEquals(800f, fast.stiffness)
        assertEquals(200f, DsMotion.spatialSlow<Float>().stiffness)
    }
}
