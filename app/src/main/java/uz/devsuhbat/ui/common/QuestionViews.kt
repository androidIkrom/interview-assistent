package uz.devsuhbat.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.devsuhbat.R
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType

/** Prompt, optional code block and the "several answers" caption of a question. */
@Composable
fun QuestionBody(question: Question, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InlineCodeText(question.prompt, style = MaterialTheme.typography.headlineSmall)
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
