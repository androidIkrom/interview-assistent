package uz.devsuhbat.ui.mock

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
import uz.devsuhbat.engine.MockResult

/** Shows the score and the per-topic breakdown. It never shows which options were correct. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockResultScreen(
    result: MockResult,
    topicTitles: Map<String, String>,
    /** False until the result is saved; the mistakes session would otherwise open without the missed questions. */
    stored: Boolean,
    onMistakes: () -> Unit,
    onHome: () -> Unit,
) {
    val missed = result.total - result.correct

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.mock_result_title)) }) },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (missed > 0) {
                    Button(onClick = onMistakes, enabled = stored, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mock_result_mistakes))
                    }
                    OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.result_home))
                    }
                } else {
                    Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.result_home))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (result.total == 0) {
                Text(stringResource(R.string.mock_result_empty), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${result.correct} / ${result.total}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.mock_result_percent, result.correct * 100 / result.total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
            }

            Text(stringResource(R.string.mock_result_topics), style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    result.topics.forEachIndexed { index, score ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = topicTitles[score.topicId] ?: score.topicId,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${score.correct} / ${score.total}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (missed > 0) {
                Text(
                    text = stringResource(R.string.mock_result_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
