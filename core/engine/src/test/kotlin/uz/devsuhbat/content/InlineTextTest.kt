package uz.devsuhbat.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineTextTest {

    private fun plain(text: String) = InlineSegment(text, code = false)
    private fun code(text: String) = InlineSegment(text, code = true)

    @Test
    fun plainText() {
        assertEquals(listOf(plain("abc")), InlineText.parse("abc"))
    }

    @Test
    fun codeInTheMiddle() {
        assertEquals(listOf(plain("a "), code("val"), plain(" b")), InlineText.parse("a `val` b"))
    }

    @Test
    fun codeAtStartAndEnd() {
        assertEquals(listOf(code("a"), plain(" va "), code("b")), InlineText.parse("`a` va `b`"))
    }

    @Test
    fun unpairedBacktickIsLiteral() {
        assertEquals(listOf(plain("a `b")), InlineText.parse("a `b"))
    }

    @Test
    fun pairThenUnpaired() {
        assertEquals(listOf(code("a"), plain(" b `c")), InlineText.parse("`a` b `c"))
    }

    @Test
    fun emptyString() {
        assertEquals(emptyList<InlineSegment>(), InlineText.parse(""))
    }

    @Test
    fun emptyCodeIsDropped() {
        val segments = InlineText.parse("a``b")

        assertEquals("ab", segments.joinToString("") { it.text })
        assertTrue(segments.none { it.code })
    }

    @Test
    fun keepsLineBreaksAndUnicode() {
        assertEquals(listOf(plain("o'zgaruvchi\n"), code("x")), InlineText.parse("o'zgaruvchi\n`x`"))
    }
}
