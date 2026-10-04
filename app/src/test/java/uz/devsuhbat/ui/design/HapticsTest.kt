package uz.devsuhbat.ui.design

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticsTest {
    @Test
    fun android11AndLaterUseConfirmAndReject() {
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, hapticConstant(HapticEvent.SELECT, 30))
        assertEquals(HapticFeedbackConstants.CONFIRM, hapticConstant(HapticEvent.CORRECT, 30))
        assertEquals(HapticFeedbackConstants.REJECT, hapticConstant(HapticEvent.WRONG, 30))
    }

    @Test
    fun olderAndroidFallsBackToOlderConstants() {
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, hapticConstant(HapticEvent.SELECT, 29))
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, hapticConstant(HapticEvent.CORRECT, 29))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, hapticConstant(HapticEvent.WRONG, 29))
    }
}
