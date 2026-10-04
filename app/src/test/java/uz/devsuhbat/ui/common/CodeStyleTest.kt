package uz.devsuhbat.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.ui.theme.JetBrainsMono
import uz.devsuhbat.ui.theme.LightExtra

class CodeStyleTest {
    // JetBrains Mono joins `==` and `===` into ligatures that look alike; questions must show each character.
    @Test
    fun inlineCodeTurnsOffLigatures() {
        val style = codeSpanStyle(LightExtra)
        assertEquals(JetBrainsMono, style.fontFamily)
        assertEquals("liga 0, calt 0", style.fontFeatureSettings)
    }

    @Test
    fun codeBlockTurnsOffLigatures() {
        assertEquals("liga 0, calt 0", CODE_FONT_FEATURES)
    }
}
