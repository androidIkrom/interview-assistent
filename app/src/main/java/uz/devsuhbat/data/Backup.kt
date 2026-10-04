package uz.devsuhbat.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** A user's whole progress and settings, as written to a backup file. */
@Serializable
data class Backup(
    val format: String = BackupCodec.FORMAT,
    val version: Int = BackupCodec.VERSION,
    /** Epoch milliseconds. */
    val exportedAt: Long,
    val settings: BackupSettings?,
    val questionStates: List<BackupQuestionState>,
    val sessions: List<BackupSession>,
    val mockTopicResults: List<BackupMockTopicResult>,
)

/** Stored settings; [level] is a content level id and [theme] a [ThemeMode] name. */
@Serializable
data class BackupSettings(
    val fieldId: String?,
    val level: String?,
    val onboardingDone: Boolean,
    val theme: String,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
)

@Serializable
data class BackupQuestionState(
    val questionId: String,
    val box: Int,
    val dueDay: Long,
    val attempts: Int,
    val wrongAttempts: Int,
    val lastAnsweredAt: Long,
)

@Serializable
data class BackupSession(
    val id: Long,
    val mode: String,
    val fieldId: String,
    val level: String,
    val startedAt: Long,
    val finishedAt: Long,
    val total: Int,
    val firstTryCorrect: Int,
)

@Serializable
data class BackupMockTopicResult(val sessionId: Long, val topicId: String, val total: Int, val correct: Int)

/** Why a backup file cannot be read. */
sealed class BackupError(message: String) : Exception(message) {
    data object Malformed : BackupError("not a readable backup")
    data object WrongFormat : BackupError("not a DevSuhbat backup")
    data object UnsupportedVersion : BackupError("backup from a newer app version")
}

object BackupCodec {
    const val FORMAT = "devsuhbat-backup"
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    /** Checks the format and version before the body, so a newer file is reported as such, not as broken. */
    fun decode(text: String): Result<Backup> {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return Result.failure(BackupError.Malformed)

        val format = runCatching { root["format"]?.jsonPrimitive?.content }.getOrNull()
        if (format != FORMAT) return Result.failure(BackupError.WrongFormat)
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: return Result.failure(BackupError.Malformed)
        if (version > VERSION) return Result.failure(BackupError.UnsupportedVersion)

        return try {
            Result.success(json.decodeFromJsonElement(Backup.serializer(), root))
        } catch (e: SerializationException) {
            Result.failure(BackupError.Malformed)
        } catch (e: IllegalArgumentException) {
            Result.failure(BackupError.Malformed)
        }
    }
}
