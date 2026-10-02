package uz.devsuhbat.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import uz.devsuhbat.content.InlineText

/** Content text where `backtick` spans are drawn as inline code. */
@Composable
fun InlineCodeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
) {
    val codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val annotated = remember(text, codeBackground) {
        val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
        buildAnnotatedString {
            InlineText.parse(text).forEach { segment ->
                if (segment.code) withStyle(codeStyle) { append(segment.text) } else append(segment.text)
            }
        }
    }
    Text(text = annotated, modifier = modifier, style = style, color = color, textDecoration = textDecoration)
}
