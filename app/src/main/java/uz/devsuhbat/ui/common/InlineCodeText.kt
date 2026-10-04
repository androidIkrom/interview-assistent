package uz.devsuhbat.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import uz.devsuhbat.content.InlineText
import uz.devsuhbat.ui.theme.ExtraColors
import uz.devsuhbat.ui.theme.JetBrainsMono
import uz.devsuhbat.ui.theme.LocalExtraColors

/** Ligatures off: JetBrains Mono would otherwise draw `==` and `===` as look-alike joined bars. */
const val CODE_FONT_FEATURES = "liga 0, calt 0"

internal fun codeSpanStyle(extra: ExtraColors) = SpanStyle(
    fontFamily = JetBrainsMono,
    fontFeatureSettings = CODE_FONT_FEATURES,
    background = extra.codeBackground,
    color = extra.codeText,
)

/** Content text where `backtick` spans are drawn as inline code. */
@Composable
fun InlineCodeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
) {
    val extra = LocalExtraColors.current
    val annotated = remember(text, extra) {
        val codeStyle = codeSpanStyle(extra)
        buildAnnotatedString {
            InlineText.parse(text).forEach { segment ->
                if (segment.code) withStyle(codeStyle) { append(segment.text) } else append(segment.text)
            }
        }
    }
    Text(text = annotated, modifier = modifier, style = style, color = color, textDecoration = textDecoration)
}
