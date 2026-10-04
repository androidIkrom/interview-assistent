package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ResultHeadlineTest {
    @Test
    fun atLeastEightyPercentIsGreat() {
        assertEquals(ResultHeadline.GREAT, ResultHeadline.of(5, 6))
        assertEquals(ResultHeadline.GREAT, ResultHeadline.of(4, 5))
    }

    @Test
    fun atLeastHalfIsGood() {
        assertEquals(ResultHeadline.GOOD, ResultHeadline.of(1, 2))
        assertEquals(ResultHeadline.GOOD, ResultHeadline.of(7, 10))
    }

    @Test
    fun belowHalfKeepsGoing() {
        assertEquals(ResultHeadline.KEEP_GOING, ResultHeadline.of(2, 5))
    }

    @Test
    fun emptySessionNeverCelebrates() {
        val headline = ResultHeadline.of(0, 0)
        assertEquals(ResultHeadline.KEEP_GOING, headline)
        assertFalse(headline.celebrates)
    }
}
