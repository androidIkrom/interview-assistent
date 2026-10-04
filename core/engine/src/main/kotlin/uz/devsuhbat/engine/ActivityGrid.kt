package uz.devsuhbat.engine

import java.time.LocalDate

/** Sessions per day for the statistics heatmap, laid out as weeks of Monday to Sunday. */
object ActivityGrid {
    const val WEEKS = 18

    /**
     * [weeks] columns, oldest first, each Monday to Sunday; the last column is today's week. An entry is the number
     * of sessions that day, or null for a day after [today]. Days outside the window are ignored.
     */
    fun of(sessionDays: List<Long>, today: Long, weeks: Int = WEEKS): List<List<Int?>> {
        val dayOfWeek = LocalDate.ofEpochDay(today).dayOfWeek.value
        val firstMonday = today - (dayOfWeek - 1) - 7L * (weeks - 1)
        val counts = sessionDays.groupingBy { it }.eachCount()
        return List(weeks) { week ->
            List(7) { weekday ->
                val day = firstMonday + 7L * week + weekday
                if (day > today) null else counts[day] ?: 0
            }
        }
    }

    /** Heat level of a day: 0, 1, 2, or 3 for three sessions or more. */
    fun level(count: Int): Int = count.coerceIn(0, 3)
}
