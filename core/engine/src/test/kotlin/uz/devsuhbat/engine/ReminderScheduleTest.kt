package uz.devsuhbat.engine

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderScheduleTest {

    private val eightPm = 20 * 60
    private val zone = ZoneId.of("Asia/Tashkent")
    private val now = LocalDateTime.of(2026, 10, 3, 21, 0).atZone(zone).toInstant()

    @Test
    fun delayUntilLaterToday() {
        assertEquals(
            Duration.ofMinutes(30),
            ReminderSchedule.delayUntilNext(LocalDateTime.of(2026, 10, 3, 19, 30), eightPm),
        )
    }

    @Test
    fun delayAtTheExactTimeIsTomorrow() {
        assertEquals(
            Duration.ofHours(24),
            ReminderSchedule.delayUntilNext(LocalDateTime.of(2026, 10, 3, 20, 0), eightPm),
        )
    }

    @Test
    fun delayAfterTheTimeIsTomorrow() {
        assertEquals(
            Duration.ofHours(20).plusMinutes(10),
            ReminderSchedule.delayUntilNext(LocalDateTime.of(2026, 10, 3, 23, 50), eightPm),
        )
    }

    @Test
    fun neverPracticedIsNotToday() {
        assertFalse(ReminderSchedule.practicedToday(null, now, zone))
    }

    @Test
    fun sessionAfterLocalMidnightCountsAsToday() {
        val justAfterMidnight = LocalDateTime.of(2026, 10, 3, 0, 5).atZone(zone).toInstant()

        assertTrue(ReminderSchedule.practicedToday(justAfterMidnight, now, zone))
    }

    @Test
    fun sessionLateYesterdayIsNotToday() {
        val lateYesterday = LocalDateTime.of(2026, 10, 2, 23, 59).atZone(zone).toInstant()

        assertFalse(ReminderSchedule.practicedToday(lateYesterday, now, zone))
    }

    @Test
    fun sessionJustFinishedIsToday() {
        assertTrue(ReminderSchedule.practicedToday(now, now, zone))
    }
}
