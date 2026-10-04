package uz.devsuhbat.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = code,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
        )
    }
}
