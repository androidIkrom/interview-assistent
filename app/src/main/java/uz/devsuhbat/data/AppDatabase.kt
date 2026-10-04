package uz.devsuhbat.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Progress on one question. The question itself lives in the content assets and is joined by id only. */
@Entity(tableName = "question_state")
data class QuestionStateEntity(
    @PrimaryKey val questionId: String,
    val box: Int,
    /** Epoch day of the local calendar on which the question returns. */
    val dueDay: Long,
    val attempts: Int,
    val wrongAttempts: Int,
    /** Epoch milliseconds. */
    val lastAnsweredAt: Long,
)

@Entity(tableName = "session_log")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** A [SessionMode] name. */
    val mode: String,
    val fieldId: String,
    /** A level id as in the content JSON, e.g. strong_middle. */
    val level: String,
    val startedAt: Long,
    val finishedAt: Long,
    val total: Int,
    val firstTryCorrect: Int,
)

/** Per-topic score of one mock interview; [sessionId] refers to [SessionLogEntity.id]. */
@Entity(tableName = "mock_topic_result", primaryKeys = ["sessionId", "topicId"])
data class MockTopicResultEntity(
    val sessionId: Long,
    val topicId: String,
    val total: Int,
    val correct: Int,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM question_state")
    fun observeStates(): Flow<List<QuestionStateEntity>>

    @Query("SELECT * FROM question_state WHERE questionId = :questionId")
    suspend fun state(questionId: String): QuestionStateEntity?

    @Upsert
    suspend fun upsert(state: QuestionStateEntity)

    /** Reads the stored state, lets [transform] compute the next one and stores it, atomically. */
    @Transaction
    suspend fun update(questionId: String, transform: (QuestionStateEntity?) -> QuestionStateEntity) {
        upsert(transform(state(questionId)))
    }

    @Insert
    suspend fun insertSession(session: SessionLogEntity): Long

    @Query("SELECT * FROM session_log ORDER BY id")
    suspend fun sessions(): List<SessionLogEntity>

    @Query("SELECT MAX(finishedAt) FROM session_log")
    suspend fun lastFinishedAt(): Long?

    @Query("SELECT * FROM session_log WHERE mode = :mode AND fieldId = :fieldId ORDER BY id DESC LIMIT 1")
    fun observeLastSession(mode: String, fieldId: String): Flow<SessionLogEntity?>

    @Insert
    suspend fun insertMockResults(results: List<MockTopicResultEntity>)

    @Query("SELECT * FROM mock_topic_result WHERE sessionId = :sessionId ORDER BY rowid")
    suspend fun mockResults(sessionId: Long): List<MockTopicResultEntity>

    /**
     * Stores a finished mock interview atomically: the session row, its per-topic scores built by [topics]
     * from the new session id, and the next state of every question in [questionIds] computed by [transform].
     */
    @Transaction
    suspend fun saveMock(
        session: SessionLogEntity,
        topics: (sessionId: Long) -> List<MockTopicResultEntity>,
        questionIds: List<String>,
        transform: (questionId: String, stored: QuestionStateEntity?) -> QuestionStateEntity,
    ): Long {
        val sessionId = insertSession(session)
        insertMockResults(topics(sessionId))
        questionIds.forEach { upsert(transform(it, state(it))) }
        return sessionId
    }

    @Query("DELETE FROM question_state")
    suspend fun deleteStates()

    @Query("DELETE FROM session_log")
    suspend fun deleteSessions()

    @Query("DELETE FROM mock_topic_result")
    suspend fun deleteMockResults()

    @Transaction
    suspend fun deleteAll() {
        deleteStates()
        deleteSessions()
        deleteMockResults()
    }

    @Query("SELECT * FROM question_state ORDER BY questionId")
    suspend fun allStates(): List<QuestionStateEntity>

    @Query("SELECT * FROM mock_topic_result ORDER BY sessionId, rowid")
    suspend fun allMockResults(): List<MockTopicResultEntity>

    @Insert
    suspend fun insertStates(states: List<QuestionStateEntity>)

    @Insert
    suspend fun insertSessions(sessions: List<SessionLogEntity>)

    /** Swaps the whole progress for the given rows in one transaction: all or nothing. */
    @Transaction
    suspend fun replaceAll(
        states: List<QuestionStateEntity>,
        sessions: List<SessionLogEntity>,
        mockResults: List<MockTopicResultEntity>,
    ) {
        deleteAll()
        insertStates(states)
        insertSessions(sessions)
        insertMockResults(mockResults)
    }
}

@Database(
    entities = [QuestionStateEntity::class, SessionLogEntity::class, MockTopicResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
