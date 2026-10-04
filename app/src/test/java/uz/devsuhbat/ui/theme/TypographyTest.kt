package uz.devsuhbat.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun headlinesUseBricolage() {
        val style = DevSuhbatTypography.headlineSmall
        assertEquals(BricolageGrotesque, style.fontFamily)
        assertEquals(22.sp, style.fontSize)
        assertEquals(FontWeight.W700, style.fontWeight)
    }

    @Test
    fun bodyUsesFigtree() {
        val style = DevSuhbatTypography.bodyLarge
        assertEquals(Figtree, style.fontFamily)
        assertEquals(16.sp, style.fontSize)
    }

    @Test
    fun labelsAndDisplayHaveTheirWeights() {
        assertEquals(FontWeight.W700, DevSuhbatTypography.labelLarge.fontWeight)
        assertEquals(FontWeight.W800, DevSuhbatTypography.displayLarge.fontWeight)
    }
}
