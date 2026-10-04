package uz.devsuhbat.ui.topics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(container: AppContainer, onOpen: (topicId: String) -> Unit) {
    val viewModel: TopicsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TopicsViewModel(container.content, container.settings.settings, container.progress.states, container.io)
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.topics_title)) })
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(stringResource(R.string.content_error))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.mixedCount == 0) {
                    item(key = "no-content") {
                        Text(
                            text = stringResource(R.string.topics_no_content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                item(key = Routes.MIXED) {
                    TopicCard(
                        title = stringResource(R.string.topics_mixed),
                        subtitle = stringResource(R.string.topics_mixed_desc),
                        questionCount = state.mixedCount,
                        percent = null,
                        highlighted = true,
                        onClick = { onOpen(Routes.MIXED) },
                    )
                }
                items(state.rows, key = { it.topic.id }) { row ->
                    TopicCard(
                        title = row.topic.title,
                        subtitle = null,
                        questionCount = row.questionCount,
                        percent = row.progress.percent,
                        highlighted = false,
                        onClick = { onOpen(row.topic.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicCard(
    title: String,
    subtitle: String?,
    questionCount: Int,
    percent: Int?,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val available = questionCount > 0
    Surface(
        onClick = onClick,
        enabled = available,
        shape = MaterialTheme.shapes.medium,
        color = if (highlighted) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().alpha(if (available) 1f else 0.5f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                val caption = when {
                    !available -> stringResource(R.string.coming_soon)
                    subtitle != null -> subtitle + " · " + stringResource(R.string.topics_count, questionCount)
                    percent != null -> stringResource(R.string.topics_count_progress, questionCount, percent)
                    else -> stringResource(R.string.topics_count, questionCount)
                }
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (available) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        }
    }
}
