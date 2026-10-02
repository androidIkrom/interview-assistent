package uz.devsuhbat.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.devsuhbat.content.Level
import uz.devsuhbat.engine.Leitner
import uz.devsuhbat.engine.MockResult
import uz.devsuhbat.engine.QuestionOutcome
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.SessionResult

enum class SessionMode { PRACTICE, MISTAKES, MOCK }

/** Score of one finished mock interview. */
data class MockSummary(val correct: Int, val total: Int)

/** The user's progress: one Leitner state per answered question, plus a log of finished sessions. */
class ProgressRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.progressDao()

    /** Today as an epoch day of the device's local calendar. */
    fun today(): Long = LocalDate.now(clock).toEpochDay()

    /** States keyed by question id. */
    val states: Flow<Map<String, QuestionState>> = dao.observeStates().map { rows ->
        rows.associate { it.questionId to it.toState() }
    }

    suspend fun record(outcome: QuestionOutcome) {
        val today = today()
        val now = clock.millis()
        dao.update(outcome.questionId) { stored ->
            Leitner.afterOutcome(stored?.toState(), outcome, today).toEntity(now)
        }
    }

    suspend fun logSession(
        mode: SessionMode,
        fieldId: String,
        level: Level,
        startedAt: Instant,
        result: SessionResult,
    ): Long = dao.insertSession(sessionRow(mode, fieldId, level, startedAt, result.total, result.firstTryCorrect))

    /** Stores a finished mock interview and applies it to the question states; returns the session id. */
    suspend fun recordMock(fieldId: String, level: Level, startedAt: Instant, result: MockResult): Long {
        val today = today()
        val now = clock.millis()
        val correctById = result.answers.associate { it.questionId to it.correct }
        return dao.saveMock(
            session = sessionRow(SessionMode.MOCK, fieldId, level, startedAt, result.total, result.correct),
            topics = { sessionId ->
                result.topics.map { MockTopicResultEntity(sessionId, it.topicId, it.total, it.correct) }
            },
            questionIds = correctById.keys.toList(),
            transform = { questionId, stored ->
                Leitner.afterMock(stored?.toState(), questionId, correctById.getValue(questionId), today).toEntity(now)
            },
        )
    }

    /** The newest mock interview of [fieldId], or null when there is none. */
    fun lastMock(fieldId: String): Flow<MockSummary?> =
        dao.observeLastSession(SessionMode.MOCK.name, fieldId).map { row ->
            row?.let { MockSummary(correct = it.firstTryCorrect, total = it.total) }
        }

    suspend fun reset() = dao.deleteAll()

    private fun sessionRow(mode: SessionMode, fieldId: String, level: Level, startedAt: Instant, total: Int, correct: Int) =
        SessionLogEntity(
            mode = mode.name,
            fieldId = fieldId,
            level = level.name.lowercase(),
            startedAt = startedAt.toEpochMilli(),
            finishedAt = clock.millis(),
            total = total,
            firstTryCorrect = correct,
        )

    private fun QuestionStateEntity.toState() = QuestionState(questionId, box, dueDay, attempts, wrongAttempts)

    private fun QuestionState.toEntity(answeredAt: Long) =
        QuestionStateEntity(questionId, box, dueDay, attempts, wrongAttempts, answeredAt)
}
