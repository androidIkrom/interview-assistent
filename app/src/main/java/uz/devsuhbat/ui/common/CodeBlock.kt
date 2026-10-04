package uz.devsuhbat.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.devsuhbat.ui.theme.JetBrainsMono

/** A multi-line code snippet. Long lines scroll sideways instead of wrapping. */
@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = code,
            fontFamily = JetBrainsMono,
            style = LocalTextStyle.current.copy(fontFeatureSettings = CODE_FONT_FEATURES),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
        )
    }
}
