package uz.devsuhbat.ui.home

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun weekDotsShrinkToFitANarrowTile() {
        // A 360 dp phone leaves 122 dp per tile: seven dots and six 5 dp gaps must all fit.
        val dot = weekDotSize(available = 122.dp, gap = 5.dp)
        assertTrue("dot $dot", dot * 7 + 5.dp * 6 <= 122.dp)
        assertEquals(16.dp, weekDotSize(available = 200.dp, gap = 5.dp))
    }

    @Test
    fun tilesStackWhenTooNarrowForTheText() {
        // 328 dp of content width on a 360 dp phone.
        assertFalse(shouldStackTiles(available = 328.dp, fontScale = 1f))
        assertTrue(shouldStackTiles(available = 328.dp, fontScale = 1.3f))
        assertFalse(shouldStackTiles(available = 379.dp, fontScale = 1.15f))
        assertTrue(shouldStackTiles(available = 379.dp, fontScale = 2f))
    }

    @Test
    fun tileContentWidthLeavesRoomForPaddingAndGap() {
        assertEquals(122.dp, tileContentWidth(available = 328.dp, stacked = false))
        assertEquals(292.dp, tileContentWidth(available = 328.dp, stacked = true))
    }
}
