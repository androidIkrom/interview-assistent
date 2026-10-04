package uz.devsuhbat.engine

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Timing of the daily practice reminder. */
object ReminderSchedule {

    /**
     * Time from [now] to the next [minutesOfDay] (minutes after local midnight). At or past that time
     * today, the next reminder is tomorrow.
     */
    fun delayUntilNext(now: LocalDateTime, minutesOfDay: Int): Duration {
        val time = LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        val today = now.toLocalDate().atTime(time)
        val next = if (today.isAfter(now)) today else today.plusDays(1)
        return Duration.between(now, next)
    }

    /** Whether a session finished at [lastFinishedAt] falls on the same local calendar day as [now]. */
    fun practicedToday(lastFinishedAt: Instant?, now: Instant, zone: ZoneId): Boolean {
        if (lastFinishedAt == null) return false
        return LocalDate.ofInstant(lastFinishedAt, zone) == LocalDate.ofInstant(now, zone)
    }
}
