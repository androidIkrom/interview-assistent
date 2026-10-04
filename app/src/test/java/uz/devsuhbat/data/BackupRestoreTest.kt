package uz.devsuhbat.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.devsuhbat.content.Level
import uz.devsuhbat.engine.QuestionOutcome
import uz.devsuhbat.engine.SessionResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRestoreTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-02T05:00:00Z"), ZoneId.of("Asia/Tashkent"))
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.progressDao()
    private val repository = ProgressRepository(db, clock)

    private val backup = Backup(
        exportedAt = 0,
        settings = null,
        questionStates = listOf(
            BackupQuestionState("android.kotlin.001", 3, 20_000, 2, 1, 1_000),
            BackupQuestionState("removed.topic.001", 1, 20_001, 1, 1, 2_000),
        ),
        sessions = listOf(BackupSession(7, "MOCK", "android", "junior", 1, 2, 25, 18)),
        mockTopicResults = listOf(BackupMockTopicResult(7, "android.kotlin", 5, 4)),
    )

    @After
    fun close() = db.close()

    private suspend fun seedExistingProgress() {
        repository.record(QuestionOutcome("android.kotlin.009", firstTryCorrect = true, wrongSubmissions = 0))
        repository.logSession(SessionMode.PRACTICE, "android", Level.JUNIOR, Instant.EPOCH, SessionResult(10, 9, 1))
    }

    @Test
    fun restoreReplacesAllRowsWithTheBackup() = runTest {
        seedExistingProgress()

        repository.restore(backup, knownQuestionIds = setOf("android.kotlin.001"))

        assertEquals(
            listOf(QuestionStateEntity("android.kotlin.001", 3, 20_000, 2, 1, 1_000)),
            repository.snapshot(settings = null).questionStates.map {
                QuestionStateEntity(it.questionId, it.box, it.dueDay, it.attempts, it.wrongAttempts, it.lastAnsweredAt)
            },
        )
        assertEquals(listOf(SessionLogEntity(7, "MOCK", "android", "junior", 1, 2, 25, 18)), dao.sessions())
        assertEquals(listOf(MockTopicResultEntity(7, "android.kotlin", 5, 4)), dao.mockResults(7))
    }

    @Test
    fun snapshotThenRestoreKeepsEveryRow() = runTest {
        seedExistingProgress()
        val snapshot = repository.snapshot(settings = null)

        repository.restore(snapshot, knownQuestionIds = setOf("android.kotlin.009"))

        assertEquals(snapshot.copy(exportedAt = 0), repository.snapshot(settings = null).copy(exportedAt = 0))
    }

    @Test
    fun malformedFileLeavesProgressUntouched() = runTest {
        seedExistingProgress()
        val before = repository.snapshot(settings = null)

        BackupCodec.decode("{").onSuccess { repository.restore(it, knownQuestionIds = emptySet()) }

        assertEquals(before, repository.snapshot(settings = null))
        assertTrue(before.questionStates.isNotEmpty())
    }
}
