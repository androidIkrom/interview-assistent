package uz.devsuhbat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {

    private val backup = Backup(
        exportedAt = 1_791_000_000_000,
        settings = BackupSettings(
            fieldId = "android",
            level = "strong_middle",
            onboardingDone = true,
            theme = "DARK",
            reminderEnabled = true,
            reminderMinutes = 8 * 60,
        ),
        questionStates = listOf(BackupQuestionState("android.kotlin.001", 3, 20_000, 2, 1, 1_790_000_000_000)),
        sessions = listOf(BackupSession(7, "MOCK", "android", "junior", 1, 2, 25, 18)),
        mockTopicResults = listOf(BackupMockTopicResult(7, "android.kotlin", 5, 4)),
    )

    private fun errorOf(json: String) = BackupCodec.decode(json).exceptionOrNull()

    @Test
    fun encodeThenDecodeReturnsTheSameBackup() {
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow())
    }

    @Test
    fun brokenJsonIsMalformed() {
        assertEquals(BackupError.Malformed, errorOf("{"))
    }

    @Test
    fun otherFormatIsRejected() {
        assertEquals(BackupError.WrongFormat, errorOf(BackupCodec.encode(backup.copy(format = "other"))))
    }

    @Test
    fun newerVersionIsRejected() {
        assertEquals(BackupError.UnsupportedVersion, errorOf(BackupCodec.encode(backup.copy(version = 2))))
    }
}
