package uz.devsuhbat.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.devsuhbat.engine.DayMark.DONE
import uz.devsuhbat.engine.DayMark.FUTURE
import uz.devsuhbat.engine.DayMark.MISSED
import uz.devsuhbat.engine.DayMark.TODAY_PENDING
import java.time.LocalDate

class StreakTest {
    /** A Wednesday. */
    private val wed = LocalDate.of(2026, 10, 7).toEpochDay()

    @Test
    fun emptyHistoryHasNoStreak() {
        val info = Streak.of(emptySet(), wed)
        assertEquals(0, info.current)
        assertEquals(0, info.longest)
    }

    @Test
    fun streakCountsBackFromToday() {
        assertEquals(3, Streak.of(setOf(wed, wed - 1, wed - 2), wed).current)
    }

    @Test
    fun pendingTodayKeepsYesterdaysStreak() {
        assertEquals(2, Streak.of(setOf(wed - 1, wed - 2), wed).current)
    }

    @Test
    fun gapBreaksTheStreak() {
        assertEquals(1, Streak.of(setOf(wed, wed - 2), wed).current)
        assertEquals(0, Streak.of(setOf(wed - 2, wed - 3), wed).current)
    }

    @Test
    fun longestIsTheLongestRunInHistory() {
        val info = Streak.of(setOf(wed - 20, wed - 19, wed - 18, wed - 10, wed), wed)
        assertEquals(3, info.longest)
        assertEquals(1, info.current)
    }

    @Test
    fun weekRunsMondayToSunday() {
        assertEquals(
            listOf(DONE, MISSED, TODAY_PENDING, FUTURE, FUTURE, FUTURE, FUTURE),
            Streak.of(setOf(wed - 2), wed).week,
        )
        assertEquals(
            listOf(DONE, MISSED, DONE, FUTURE, FUTURE, FUTURE, FUTURE),
            Streak.of(setOf(wed - 2, wed), wed).week,
        )
    }
}
