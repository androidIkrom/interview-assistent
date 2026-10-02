package uz.devsuhbat.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.devsuhbat.content.Level
import uz.devsuhbat.engine.Leitner
import uz.devsuhbat.engine.QuestionOutcome
import uz.devsuhbat.engine.QuestionState
import uz.devsuhbat.engine.SessionResult

enum class SessionMode { PRACTICE, MISTAKES, MOCK }

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
            val next = Leitner.afterOutcome(stored?.toState(), outcome, today)
            QuestionStateEntity(next.questionId, next.box, next.dueDay, next.attempts, next.wrongAttempts, now)
        }
    }

    suspend fun logSession(
        mode: SessionMode,
        fieldId: String,
        level: Level,
        startedAt: Instant,
        result: SessionResult,
    ): Long = dao.insertSession(
        SessionLogEntity(
            mode = mode.name,
            fieldId = fieldId,
            level = level.name.lowercase(),
            startedAt = startedAt.toEpochMilli(),
            finishedAt = clock.millis(),
            total = result.total,
            firstTryCorrect = result.firstTryCorrect,
        )
    )

    suspend fun reset() = dao.deleteAll()

    private fun QuestionStateEntity.toState() = QuestionState(questionId, box, dueDay, attempts, wrongAttempts)
}
