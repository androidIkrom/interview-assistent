package uz.devsuhbat.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavMotionTest {
    @Test
    fun tabsFadeBetweenEachOther() {
        assertEquals(NavMotion.FADE, navMotion(Routes.HOME, Routes.TOPICS))
    }

    @Test
    fun screensAboveTabsSlide() {
        assertEquals(NavMotion.SLIDE, navMotion(Routes.HOME, Routes.SESSION))
        assertEquals(NavMotion.SLIDE, navMotion(Routes.SESSION, Routes.HOME))
        assertEquals(NavMotion.SLIDE, navMotion(Routes.HOME, Routes.SETTINGS))
    }

    @Test
    fun onlyTabsAreTopLevel() {
        assertTrue(isTopLevel(Routes.HOME))
        assertTrue(isTopLevel(Routes.TOPICS))
        assertFalse(isTopLevel(Routes.SESSION))
        assertFalse(isTopLevel(null))
    }

    @Test
    fun onlyTabsReserveRoomForTheBar() {
        // A screen above the tabs takes the full height at once, so it does not jump while the bar slides away.
        assertEquals(80.dp, contentBottomPadding(Routes.HOME, barHeight = 80.dp))
        assertEquals(0.dp, contentBottomPadding(Routes.SESSION, barHeight = 80.dp))
    }

    @Test
    fun statsIsATab() {
        assertTrue(isTopLevel(Routes.STATS))
        assertEquals(NavMotion.FADE, navMotion(Routes.STATS, Routes.HOME))
        assertEquals(80.dp, contentBottomPadding(Routes.STATS, barHeight = 80.dp))
    }

    @Test
    fun aSessionReturnsToTheTabItWasOpenedFrom() {
        assertEquals(Routes.STATS, sessionReturnRoute(Routes.STATS))
        assertEquals(Routes.TOPICS, sessionReturnRoute(Routes.TOPICS))
        assertEquals(Routes.HOME, sessionReturnRoute(Routes.HOME))
        // Opened from somewhere else, e.g. the mistakes session after a mock: back to Home.
        assertEquals(Routes.HOME, sessionReturnRoute(Routes.MOCK))
        assertEquals(Routes.HOME, sessionReturnRoute(null))
    }
}
