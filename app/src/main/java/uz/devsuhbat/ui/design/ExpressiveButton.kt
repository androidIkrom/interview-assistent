package uz.devsuhbat.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.devsuhbat.ui.theme.LocalExtraColors

enum class ButtonTone { PRIMARY, SUCCESS, OUTLINED }

/** Full-width action button that squeezes and squares its corners while pressed. */
@Composable
fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.PRIMARY,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    /** Overrides [tone]'s colours, e.g. an inverted button on a primary-coloured card. */
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 16.dp else 28.dp, DsMotion.spatialFast(), label = "corner")
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, DsMotion.spatialFast(), label = "scale")

    val (container, content) = when {
        !enabled -> colors.onSurface.copy(alpha = 0.12f) to colors.onSurface.copy(alpha = 0.38f)
        containerColor != null && contentColor != null -> containerColor to contentColor
        tone == ButtonTone.PRIMARY -> colors.primary to colors.onPrimary
        tone == ButtonTone.SUCCESS -> extra.success to extra.onSuccess
        else -> Color.Transparent to colors.primary
    }
    val border = if (tone == ButtonTone.OUTLINED && enabled) BorderStroke(1.5.dp, colors.outline) else null

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        border = border,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
