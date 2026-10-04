package uz.devsuhbat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    private fun textPairs(colors: ColorScheme, extra: ExtraColors): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary/primary", colors.onPrimary, colors.primary),
        Triple("onPrimaryContainer/primaryContainer", colors.onPrimaryContainer, colors.primaryContainer),
        Triple("onSurface/background", colors.onSurface, colors.background),
        Triple("onSurface/surfaceContainerLowest", colors.onSurface, colors.surfaceContainerLowest),
        Triple("onSurfaceVariant/surfaceContainerLowest", colors.onSurfaceVariant, colors.surfaceContainerLowest),
        Triple("onErrorContainer/errorContainer", colors.onErrorContainer, colors.errorContainer),
        Triple("error/surfaceContainerLowest", colors.error, colors.surfaceContainerLowest),
        Triple("onSuccess/success", extra.onSuccess, extra.success),
        Triple("onSuccessContainer/successContainer", extra.onSuccessContainer, extra.successContainer),
        Triple("onStreakContainer/streakContainer", extra.onStreakContainer, extra.streakContainer),
        Triple("codeText/codeBackground", extra.codeText, extra.codeBackground),
    )

    private fun assertReadable(colors: ColorScheme, extra: ExtraColors) {
        textPairs(colors, extra).forEach { (name, text, background) ->
            val ratio = contrast(text, background)
            assertTrue("$name contrast is $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun lightThemeTextIsReadable() = assertReadable(LightColors, LightExtra)

    @Test
    fun darkThemeTextIsReadable() = assertReadable(DarkColors, DarkExtra)

    @Test
    fun paletteUsesTheSpecValues() {
        assertEquals(Color(0xFF4433D1), LightColors.primary)
        assertEquals(Color(0xFF131218), DarkColors.background)
        assertEquals(Color(0xFFF07A2B), LightExtra.streak)
    }
}
