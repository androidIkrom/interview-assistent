package uz.devsuhbat.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.devsuhbat.content.ContentStore
import uz.devsuhbat.content.Field
import uz.devsuhbat.content.Level
import uz.devsuhbat.data.SettingsRepository

/** [available] is false while none of the field's topics has questions yet. */
data class FieldRow(val field: Field, val available: Boolean)

data class FieldGroup(val group: String, val rows: List<FieldRow>)

enum class OnboardingStep { FIELD, LEVEL }

data class OnboardingUiState(
    val loading: Boolean = true,
    val groups: List<FieldGroup> = emptyList(),
    val fieldId: String? = null,
    val level: Level? = null,
    val step: OnboardingStep = OnboardingStep.FIELD,
)

class OnboardingViewModel(
    private val content: ContentStore,
    private val settings: SettingsRepository,
    private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val groups = withContext(io) { loadGroups() }
            val current = settings.settings.first()
            val availableIds = groups.flatMap { it.rows }.filter { it.available }.map { it.field.id }
            _state.value = OnboardingUiState(
                loading = false,
                groups = groups,
                fieldId = current.fieldId?.takeIf { it in availableIds },
                level = current.level,
            )
        }
    }

    private fun loadGroups(): List<FieldGroup> {
        val rows = content.catalog()?.fields.orEmpty().map { field ->
            val available = content.topicsOf(field.id).any { content.questions(it.id).isNotEmpty() }
            FieldRow(field, available)
        }
        // groupBy keeps first-seen order, so groups and fields stay in catalog order.
        return rows.groupBy { it.field.group }.map { (group, groupRows) -> FieldGroup(group, groupRows) }
    }

    fun selectField(fieldId: String) = _state.update { it.copy(fieldId = fieldId) }

    fun selectLevel(level: Level) = _state.update { it.copy(level = level) }

    fun showLevels() = _state.update { if (it.fieldId != null) it.copy(step = OnboardingStep.LEVEL) else it }

    fun showFields() = _state.update { it.copy(step = OnboardingStep.FIELD) }

    fun confirm(onDone: () -> Unit) {
        val fieldId = _state.value.fieldId ?: return
        val level = _state.value.level ?: return
        viewModelScope.launch {
            settings.completeOnboarding(fieldId, level)
            onDone()
        }
    }
}
