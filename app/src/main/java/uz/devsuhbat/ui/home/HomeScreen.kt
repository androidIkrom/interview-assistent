package uz.devsuhbat.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.data.MockSummary
import uz.devsuhbat.engine.NextReview
import uz.devsuhbat.engine.QuestionPicker
import uz.devsuhbat.ui.common.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onPractice: () -> Unit,
    onMistakes: () -> Unit,
    onMock: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    content = container.content,
                    settings = container.settings.settings,
                    states = container.progress.states,
                    today = container.progress::today,
                    io = container.io,
                    lastMock = container.progress::lastMock,
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReadinessCard(state)
            MistakesCard(state.dueCount, state.nextReview, onMistakes)
            PracticeCard(onPractice)
            MockCard(state.mockQuestionCount, state.lastMock, onMock)
        }
    }
}

@Composable
private fun MockCard(questionCount: Int, lastMock: MockSummary?, onMock: () -> Unit) {
    val available = questionCount >= QuestionPicker.MOCK_MIN
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_mock), style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (available) {
                    stringResource(R.string.home_mock_desc, questionCount)
                } else {
                    stringResource(R.string.home_mock_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lastMock != null) {
                Text(
                    text = stringResource(R.string.home_mock_last, lastMock.correct, lastMock.total),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Button(onClick = onMock, enabled = available, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start))
            }
        }
    }
}

@Composable
private fun ReadinessCard(state: HomeUiState) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.home_your_target), style = MaterialTheme.typography.labelLarge, color = onContainer)
            Text(
                text = state.fieldTitle.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                color = onContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            state.level?.let { level ->
                Text(stringResource(level.titleRes), style = MaterialTheme.typography.titleMedium, color = onContainer)
            }
            if (state.readiness.total > 0) {
                Text(
                    text = stringResource(R.string.home_readiness, state.readiness.percent),
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer,
                    modifier = Modifier.padding(top = 16.dp),
                )
                LinearProgressIndicator(
                    progress = { state.readiness.mastered.toFloat() / state.readiness.total },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    drawStopIndicator = {},
                )
                Text(
                    text = stringResource(R.string.home_mastered, state.readiness.mastered, state.readiness.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer,
                )
            }
        }
    }
}

@Composable
private fun MistakesCard(dueCount: Int, nextReview: NextReview?, onMistakes: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_mistakes), style = MaterialTheme.typography.titleLarge)
            Text(
                text = when {
                    dueCount > 0 -> stringResource(R.string.home_mistakes_due, dueCount)
                    nextReview == null -> stringResource(R.string.home_mistakes_none)
                    nextReview.inDays == 1 -> stringResource(R.string.home_mistakes_tomorrow, nextReview.count)
                    else -> stringResource(R.string.home_mistakes_later, nextReview.count, nextReview.inDays)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onMistakes, enabled = dueCount > 0, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_mistakes_action))
            }
        }
    }
}

@Composable
private fun PracticeCard(onPractice: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_practice), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.home_practice_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_practice))
            }
        }
    }
}
