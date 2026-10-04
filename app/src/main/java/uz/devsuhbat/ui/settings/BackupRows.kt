package uz.devsuhbat.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.data.Backup
import uz.devsuhbat.data.BackupCodec
import uz.devsuhbat.data.BackupError
import uz.devsuhbat.reminder.ReminderScheduler

/** "Export progress" and "Import progress" rows; results are reported through [onMessage]. */
@Composable
fun BackupRows(container: AppContainer, onMessage: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Backup?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(container.io) {
                try {
                    val backup = container.progress.snapshot(container.settings.backup())
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
                    stream.bufferedWriter().use { it.write(BackupCodec.encode(backup)) }
                    true
                } catch (e: IOException) {
                    false
                }
            }
            onMessage(if (ok) R.string.backup_exported else R.string.backup_write_failed)
        }
    }

    fun read(uri: Uri) {
        scope.launch {
            val result = withContext(container.io) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text == null) Result.failure(BackupError.Malformed) else BackupCodec.decode(text)
                } catch (e: IOException) {
                    Result.failure(BackupError.Malformed)
                }
            }
            result
                .onSuccess { pending = it }
                .onFailure { error ->
                    onMessage(
                        when (error) {
                            BackupError.WrongFormat -> R.string.backup_wrong_format
                            BackupError.UnsupportedVersion -> R.string.backup_newer_version
                            else -> R.string.backup_malformed
                        }
                    )
                }
        }
    }

    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) read(uri)
    }

    pending?.let { backup ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_import)) },
            text = { Text(stringResource(R.string.backup_import_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        withContext(container.io) {
                            val content = container.content
                            val known = content.catalog()?.topics.orEmpty()
                                .flatMap { topic -> content.questions(topic.id) }
                                .mapTo(HashSet()) { it.id }
                            container.progress.restore(backup, known)
                            backup.settings?.let { container.settings.restore(it) }
                        }
                        backup.settings?.let { ReminderScheduler.apply(context, it.reminderEnabled, it.reminderMinutes) }
                        onMessage(R.string.backup_imported)
                    }
                }) { Text(stringResource(R.string.backup_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Text(
        text = stringResource(R.string.backup_export),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { export.launch("devsuhbat-backup-${LocalDate.now()}.json") }
            .padding(16.dp),
    )
    Text(
        text = stringResource(R.string.backup_import),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { import.launch(arrayOf("application/json", "text/plain")) }
            .padding(16.dp),
    )
}
