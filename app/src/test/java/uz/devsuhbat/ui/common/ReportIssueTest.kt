package uz.devsuhbat.ui.common

import org.junit.Assert.assertTrue
import org.junit.Test

class ReportIssueTest {
    @Test
    fun reportTextNamesTheQuestionAndAppVersion() {
        val text = issueReportText("android.kotlin.003", "Bu kod nima chiqaradi?", "1.0.0")

        assertTrue(text, text.contains("android.kotlin.003"))
        assertTrue(text, text.contains("Bu kod nima chiqaradi?"))
        assertTrue(text, text.contains("DevSuhbat 1.0.0"))
        assertTrue(text, text.lines().last() == "Izoh: ")
    }
}
