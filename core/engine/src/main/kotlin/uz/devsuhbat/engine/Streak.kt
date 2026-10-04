package uz.devsuhbat.engine

import java.time.LocalDate

/** How one day of the current week looks in the streak row. */
enum class DayMark { DONE, TODAY_PENDING, MISSED, FUTURE }

/** [week] holds Monday to Sunday of today's week. */
data class StreakInfo(val current: Int, val longest: Int, val week: List<DayMark>)

/**
 * Consecutive days with at least one finished session. Days are epoch days of the local calendar.
 * Today without a session yet does not break the streak: it still counts from yesterday.
 */
object Streak {
    val NONE = StreakInfo(current = 0, longest = 0, week = emptyList())

    fun of(activeDays: Set<Long>, today: Long): StreakInfo {
        val start = when {
            today in activeDays -> today
            today - 1 in activeDays -> today - 1
            else -> null
        }
        var current = 0
        if (start != null) {
            var day = start
            while (day in activeDays) {
                current++
                day--
            }
        }

        var longest = 0
        var run = 0
        var previous: Long? = null
        for (day in activeDays.sorted()) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }

        val monday = today - (LocalDate.ofEpochDay(today).dayOfWeek.value - 1)
        val week = (0 until 7).map { offset ->
            val day = monday + offset
            when {
                day > today -> DayMark.FUTURE
                day in activeDays -> DayMark.DONE
                day == today -> DayMark.TODAY_PENDING
                else -> DayMark.MISSED
            }
        }
        return StreakInfo(current, longest, week)
    }
}
