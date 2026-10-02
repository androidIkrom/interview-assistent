package uz.devsuhbat.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import uz.devsuhbat.content.Level
import uz.devsuhbat.ui.common.descriptionRes
import uz.devsuhbat.ui.common.groupTitleRes
import uz.devsuhbat.ui.common.titleRes

/**
 * Field and level picker. [onBack] is null on first launch, where there is nothing to go back to;
 * from Settings it closes the screen without saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(container: AppContainer, onDone: () -> Unit, onBack: (() -> Unit)?) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OnboardingViewModel(container.content, container.settings, container.io) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editing = onBack != null
    val onLevels = state.step == OnboardingStep.LEVEL

    BackHandler(enabled = onLevels) { viewModel.showFields() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (onLevels) R.string.onboarding_level_title else R.string.onboarding_field_title))
                },
                navigationIcon = {
                    val back: (() -> Unit)? = if (onLevels) viewModel::showFields else onBack
                    if (back != null) {
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                if (onLevels) {
                    Button(
                        onClick = { viewModel.confirm(onDone) },
                        enabled = state.level != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (editing) R.string.action_save else R.string.action_start))
                    }
                } else {
                    Button(
                        onClick = viewModel::showLevels,
                        enabled = state.fieldId != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.groups.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(stringResource(R.string.content_error))
            }
            onLevels -> LevelList(state.level, viewModel::selectLevel, padding)
            else -> FieldList(state.groups, state.fieldId, viewModel::selectField, padding)
        }
    }
}

@Composable
private fun FieldList(
    groups: List<FieldGroup>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "group:${group.group}") {
                Text(
                    text = stringResource(groupTitleRes(group.group)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(group.rows, key = { it.field.id }) { row ->
                ChoiceCard(
                    title = row.field.title,
                    subtitle = if (row.available) null else stringResource(R.string.coming_soon),
                    selected = row.field.id == selectedId,
                    enabled = row.available,
                    onClick = { onSelect(row.field.id) },
                )
            }
        }
    }
}

@Composable
private fun LevelList(selected: Level?, onSelect: (Level) -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_level_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Level.entries.forEach { level ->
            ChoiceCard(
                title = stringResource(level.titleRes),
                subtitle = stringResource(level.descriptionRes),
                selected = level == selected,
                enabled = true,
                onClick = { onSelect(level) },
            )
        }
    }
}

@Composable
private fun ChoiceCard(title: String, subtitle: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
