package uz.devsuhbat.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.devsuhbat.ui.theme.DarkColors
import uz.devsuhbat.ui.theme.DarkExtra
import uz.devsuhbat.ui.theme.LightColors
import uz.devsuhbat.ui.theme.LightExtra
import kotlin.math.max
import kotlin.math.min

class OptionCardFixesTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun everyStateHasReadableTextInBothThemes() {
        listOf(LightColors to LightExtra, DarkColors to DarkExtra).forEach { (colors, extra) ->
            OptionState.entries.forEach { state ->
                val palette = optionPalette(state, colors, extra)
                val ratio = contrast(palette.content, palette.container)
                assertTrue("$state text contrast is $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun correctOptionUsesTheSuccessTextColour() {
        assertEquals(DarkExtra.onSuccessContainer, optionPalette(OptionState.CORRECT, DarkColors, DarkExtra).content)
    }

    /** Advances one 16 ms frame per call on the test's virtual time. */
    private class TestFrameClock : MonotonicFrameClock {
        private var nanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16)
            nanos += 16_000_000L
            return onFrame(nanos)
        }
    }

    @Test
    fun cancelledShakeReturnsTheCardToRest() = runTest {
        val shake = Animatable(0f)
        val pop = Animatable(1f)
        val job = launch(TestFrameClock()) { playOptionMotion(OptionState.SELECTED, OptionState.WRONG, shake, pop, 30f) }
        advanceTimeBy(100)
        runCurrent()
        assertNotEquals(0f, shake.value)

        job.cancelAndJoin()

        assertEquals(0f, shake.value)
    }

    @Test
    fun cancelledPopReturnsTheCardToRest() = runTest {
        val shake = Animatable(0f)
        val pop = Animatable(1f)
        val job = launch(TestFrameClock()) { playOptionMotion(OptionState.SELECTED, OptionState.CORRECT, shake, pop, 30f) }
        advanceTimeBy(50)
        runCurrent()
        assertNotEquals(1f, pop.value)

        job.cancelAndJoin()

        assertEquals(1f, pop.value)
    }
}
