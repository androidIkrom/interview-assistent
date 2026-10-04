package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ActivityGridTest {
    /** A Wednesday. */
    private val wed = LocalDate.of(2026, 10, 7).toEpochDay()

    @Test
    fun gridHasEighteenWeeksOfSevenDays() {
        val grid = ActivityGrid.of(emptyList(), wed)
        assertEquals(18, grid.size)
        grid.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun todaySitsInTheLastColumnOnItsWeekday() {
        val grid = ActivityGrid.of(listOf(wed, wed, wed - 2), wed)
        val last = grid.last()
        assertEquals(1, last[0])
        assertEquals(0, last[1])
        assertEquals(2, last[2])
        (3..6).forEach { assertNull(last[it]) }
        grid.dropLast(1).forEach { week -> week.forEach { assertEquals(0, it) } }
    }

    @Test
    fun daysOutsideTheWindowAreIgnored() {
        val grid = ActivityGrid.of(listOf(wed - 200), wed)
        assertEquals(0, grid.flatten().sumOf { it ?: 0 })
    }

    @Test
    fun levelsCapAtThree() {
        assertEquals(listOf(0, 1, 2, 3, 3), (0..4).map(ActivityGrid::level))
    }
}
