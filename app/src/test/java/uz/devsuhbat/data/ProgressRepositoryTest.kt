package uz.devsuhbat.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.devsuhbat.content.Level
import uz.devsuhbat.engine.MockAnswer
import uz.devsuhbat.engine.MockResult
import uz.devsuhbat.engine.QuestionOutcome
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.SessionResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRepositoryTest {

    private val zone = ZoneId.of("Asia/Tashkent")
    private val clock = Clock.fixed(Instant.parse("2026-10-02T05:00:00Z"), zone)
    private val today = LocalDate.of(2026, 10, 2).toEpochDay()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = ProgressRepository(db, clock)

    @After
    fun close() = db.close()

    private fun firstTry(id: String) = QuestionOutcome(id, firstTryCorrect = true, wrongSubmissions = 0)
    private fun missed(id: String) = QuestionOutcome(id, firstTryCorrect = false, wrongSubmissions = 1)

    @Test
    fun todayIsTheLocalCalendarDay() {
        val lateEvening = ProgressRepository(db, Clock.fixed(Instant.parse("2026-10-02T18:30:00Z"), zone))
        val afterMidnight = ProgressRepository(db, Clock.fixed(Instant.parse("2026-10-02T19:30:00Z"), zone))

        assertEquals(today, repository.today())
        assertEquals(today, lateEvening.today())
        assertEquals(today + 1, afterMidnight.today())
    }

    @Test
    fun statesStartEmpty() = runTest {
        assertEquals(emptyMap<String, QuestionState>(), repository.states.first())
    }

    @Test
    fun recordStoresFirstTryInBoxThree() = runTest {
        repository.record(firstTry("q.001"))

        assertEquals(
            mapOf("q.001" to QuestionState("q.001", box = 3, dueDay = today + 7, attempts = 1, wrongAttempts = 0)),
            repository.states.first(),
        )
    }

    @Test
    fun recordAppliesLeitnerToTheStoredState() = runTest {
        repository.record(missed("q.001"))
        repository.record(firstTry("q.001"))

        assertEquals(
            QuestionState("q.001", box = 2, dueDay = today + 3, attempts = 2, wrongAttempts = 1),
            repository.states.first()["q.001"],
        )
    }

    @Test
    fun logSessionStoresARow() = runTest {
        val startedAt = Instant.parse("2026-10-02T04:50:00Z")

        val id = repository.logSession(
            SessionMode.PRACTICE, "android", Level.STRONG_MIDDLE, startedAt,
            SessionResult(total = 10, firstTryCorrect = 7, reworked = 3),
        )

        assertTrue(id > 0)
        val row = db.progressDao().sessions().single()
        assertEquals("PRACTICE", row.mode)
        assertEquals("android", row.fieldId)
        assertEquals("strong_middle", row.level)
        assertEquals(startedAt.toEpochMilli(), row.startedAt)
        assertEquals(clock.millis(), row.finishedAt)
        assertEquals(10, row.total)
        assertEquals(7, row.firstTryCorrect)
    }

    @Test
    fun resetClearsEverything() = runTest {
        repository.record(firstTry("q.001"))
        repository.logSession(SessionMode.MISTAKES, "android", Level.JUNIOR, Instant.EPOCH, SessionResult(1, 1, 0))

        repository.reset()

        assertEquals(emptyMap<String, QuestionState>(), repository.states.first())
        assertEquals(emptyList<SessionLogEntity>(), db.progressDao().sessions())
    }

    // --- mock interviews ---

    private val mockResult = MockResult(
        listOf(
            MockAnswer("a.x.001", "a.x", correct = true),
            MockAnswer("a.x.002", "a.x", correct = false),
            MockAnswer("a.y.001", "a.y", correct = false),
        )
    )

    @Test
    fun recordMockStoresSessionAndTopicScores() = runTest {
        val id = repository.recordMock("android", Level.MIDDLE, Instant.parse("2026-10-02T04:30:00Z"), mockResult)

        val row = db.progressDao().sessions().single()
        assertEquals(id, row.id)
        assertEquals("MOCK", row.mode)
        assertEquals("middle", row.level)
        assertEquals(3, row.total)
        assertEquals(1, row.firstTryCorrect)
        assertEquals(
            listOf(MockTopicResultEntity(id, "a.x", total = 2, correct = 1), MockTopicResultEntity(id, "a.y", total = 1, correct = 0)),
            db.progressDao().mockResults(id),
        )
    }

    @Test
    fun recordMockSendsMissedQuestionsToToday() = runTest {
        repository.recordMock("android", Level.MIDDLE, Instant.EPOCH, mockResult)

        val states = repository.states.first()
        assertEquals(QuestionState("a.x.001", box = 3, dueDay = today + 7, attempts = 1, wrongAttempts = 0), states["a.x.001"])
        assertEquals(QuestionState("a.x.002", box = 1, dueDay = today, attempts = 1, wrongAttempts = 1), states["a.x.002"])
        assertEquals(QuestionState("a.y.001", box = 1, dueDay = today, attempts = 1, wrongAttempts = 1), states["a.y.001"])
    }

    @Test
    fun lastMockIsTheNewestOfTheField() = runTest {
        val perfect = MockResult(listOf(MockAnswer("a.x.001", "a.x", correct = true)))
        repository.recordMock("android", Level.MIDDLE, Instant.EPOCH, mockResult)
        repository.recordMock("android", Level.MIDDLE, Instant.EPOCH, perfect)
        repository.recordMock("ios", Level.MIDDLE, Instant.EPOCH, mockResult)
        repository.logSession(SessionMode.PRACTICE, "android", Level.MIDDLE, Instant.EPOCH, SessionResult(10, 9, 1))

        assertEquals(MockSummary(correct = 1, total = 1), repository.lastMock("android").first())
        assertEquals(MockSummary(correct = 1, total = 3), repository.lastMock("ios").first())
        assertEquals(null, repository.lastMock("qa").first())
    }

    @Test
    fun lastFinishedAtIsNullWithoutSessions() = runTest {
        assertNull(repository.lastFinishedAt())
    }

    @Test
    fun lastFinishedAtIsTheNewestSession() = runTest {
        val later = ProgressRepository(db, Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), zone))
        later.logSession(SessionMode.PRACTICE, "android", Level.JUNIOR, Instant.EPOCH, SessionResult(10, 9, 1))
        repository.logSession(SessionMode.MISTAKES, "android", Level.JUNIOR, Instant.EPOCH, SessionResult(1, 1, 0))

        assertEquals(Instant.parse("2026-10-02T12:00:00Z"), repository.lastFinishedAt())
    }
}
