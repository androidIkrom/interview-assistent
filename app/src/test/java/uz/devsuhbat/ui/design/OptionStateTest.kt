package uz.devsuhbat.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

class OptionStateTest {
    @Test
    fun onlyIdleAndSelectedAreInteractive() {
        val interactive = OptionState.entries.filter { it.interactive }
        assertEquals(listOf(OptionState.IDLE, OptionState.SELECTED), interactive)
    }

    @Test
    fun shakesOnlyOnTransitionIntoWrong() {
        assertEquals(true, shouldShake(OptionState.SELECTED, OptionState.WRONG))
        assertEquals(true, shouldShake(OptionState.IDLE, OptionState.WRONG))
        assertEquals(false, shouldShake(OptionState.WRONG, OptionState.WRONG))
        // First composition, e.g. the card scrolled back into view: no shake.
        assertEquals(false, shouldShake(null, OptionState.WRONG))
        assertEquals(false, shouldShake(OptionState.WRONG, OptionState.IDLE))
    }
}
