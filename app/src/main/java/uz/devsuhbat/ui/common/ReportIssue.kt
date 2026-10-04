package uz.devsuhbat.ui.common

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import uz.devsuhbat.R
import uz.devsuhbat.content.Question

/** Text of a mistake report: app version, question id and prompt, then a line for the user's comment. */
fun issueReportText(questionId: String, prompt: String, versionName: String): String =
    "DevSuhbat $versionName\nSavol: $questionId\n$prompt\n\nIzoh: "

/** Opens the system share sheet with [text], so the user picks where to send the report. */
fun shareIssueReport(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.report_issue_chooser)))
}

private fun appVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/** Top bar action that reports a mistake in [question]; hidden while no question is shown. */
@Composable
fun ReportIssueAction(question: Question?) {
    if (question == null) return
    val context = LocalContext.current
    IconButton(onClick = {
        shareIssueReport(context, issueReportText(question.id, question.prompt, appVersionName(context)))
    }) {
        Icon(Icons.Outlined.Flag, stringResource(R.string.report_issue))
    }
}
