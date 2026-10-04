package uz.devsuhbat.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.reminder.ReminderScheduler

/** Daily reminder switch and time. Turning it on asks for the notification permission on Android 13+. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettings(container: AppContainer, enabled: Boolean, minutes: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var denied by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    fun save(on: Boolean, at: Int) {
        scope.launch {
            container.settings.setReminder(on, at)
            ReminderScheduler.apply(context, on, at)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) save(true, minutes)
    }

    fun turnOn() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            denied = false
            save(true, minutes)
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    pickTime = false
                    save(enabled, state.hour * 60 + state.minute)
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = { on -> if (on) turnOn() else save(false, minutes) },
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.reminder_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (denied) R.string.reminder_denied else R.string.reminder_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = if (denied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { pickTime = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        Text(
            text = stringResource(R.string.reminder_time),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
