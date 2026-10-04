package uz.devsuhbat.ui

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
}
