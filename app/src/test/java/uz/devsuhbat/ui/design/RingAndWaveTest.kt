package uz.devsuhbat.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RingAndWaveTest {
    @Test
    fun ringLabelSizeIgnoresFontScale() {
        assertEquals(26.4f, ringLabelSizeSp(120f, 1f), 0.01f)
        assertEquals(13.2f, ringLabelSizeSp(120f, 2f), 0.01f)
    }

    @Test
    fun trackStaysInsideTheCanvas() {
        assertEquals(2.5f to 197.5f, trackSegment(width = 200f, activeEnd = 0f, gap = 10f, stroke = 5f))
    }

    @Test
    fun trackStartsAfterTheGap() {
        assertEquals(60f to 197.5f, trackSegment(width = 200f, activeEnd = 50f, gap = 10f, stroke = 5f))
    }

    @Test
    fun noTrackWhenTheWaveIsFull() {
        assertNull(trackSegment(width = 200f, activeEnd = 195f, gap = 10f, stroke = 5f))
    }
}
