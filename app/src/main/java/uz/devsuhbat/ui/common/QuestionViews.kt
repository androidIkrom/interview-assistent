package uz.devsuhbat.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import uz.devsuhbat.R
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.ui.theme.LocalExtraColors

/** Prompt, optional code block and the "several answers" caption of a question. */
@Composable
fun QuestionBody(question: Question, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InlineCodeText(question.prompt, style = MaterialTheme.typography.titleMedium)
        question.code?.let { CodeBlock(it) }
        if (question.type == QuestionType.MULTI) {
            Text(
                text = stringResource(R.string.session_multi_caption),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * One answer option. Until [solved] it only shows "selected" or "eliminated"; nothing marks a correct one.
 * Once solved, the selected options are the correct set and are drawn in the success colour.
 */
@Composable
fun OptionCard(
    option: Option,
    multi: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    eliminated: Boolean = false,
    solved: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val confirmed = solved && selected
    val container = when {
        confirmed -> extra.successContainer
        selected -> colors.primaryContainer
        else -> colors.surface
    }
    val border = when {
        confirmed -> extra.success
        selected -> colors.primary
        else -> colors.outline.copy(alpha = 0.4f)
    }
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = !eliminated && !solved,
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (eliminated) 0.45f else 1f)
            .semantics { role = if (multi) Role.Checkbox else Role.RadioButton },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multi) {
                Checkbox(checked = selected, onCheckedChange = null, enabled = !eliminated)
            } else {
                RadioButton(selected = selected, onClick = null, enabled = !eliminated)
            }
            InlineCodeText(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (eliminated) TextDecoration.LineThrough else null,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
