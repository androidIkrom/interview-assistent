package uz.devsuhbat.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.devsuhbat.R
import uz.devsuhbat.engine.SessionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultScreen(result: SessionResult, onAgain: () -> Unit, onHome: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.result_title)) }) },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.result_again))
                }
                OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.result_home))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (result.total == 0) {
                Text(stringResource(R.string.result_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    text = "${result.firstTryCorrect} / ${result.total}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        ResultRow(stringResource(R.string.result_total), result.total)
                        HorizontalDivider()
                        ResultRow(stringResource(R.string.result_first_try), result.firstTryCorrect)
                        HorizontalDivider()
                        ResultRow(stringResource(R.string.result_reworked), result.reworked)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}
