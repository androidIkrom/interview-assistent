package uz.devsuhbat.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFF4433D1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3DEFF),
    onPrimaryContainer = Color(0xFF1A0F6B),
    secondary = Color(0xFF57556A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3DEFF),
    onSecondaryContainer = Color(0xFF1A0F6B),
    background = Color(0xFFF6F4FD),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFF6F4FD),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFEEEBFA),
    onSurfaceVariant = Color(0xFF57556A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEEBFA),
    surfaceContainerHigh = Color(0xFFEEEBFA),
    surfaceContainerHighest = Color(0xFFE7E3F5),
    outline = Color(0xFFD9D5EA),
    outlineVariant = Color(0xFFE2DEF0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF23157A),
    primaryContainer = Color(0xFF3A2CB8),
    onPrimaryContainer = Color(0xFFE3DEFF),
    secondary = Color(0xFFC8C4D4),
    onSecondary = Color(0xFF23157A),
    secondaryContainer = Color(0xFF3A2CB8),
    onSecondaryContainer = Color(0xFFE3DEFF),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE6E1EC),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE6E1EC),
    surfaceVariant = Color(0xFF24232B),
    onSurfaceVariant = Color(0xFFC8C4D4),
    surfaceContainerLowest = Color(0xFF1C1B22),
    surfaceContainerLow = Color(0xFF1C1B22),
    surfaceContainer = Color(0xFF24232B),
    surfaceContainerHigh = Color(0xFF24232B),
    surfaceContainerHighest = Color(0xFF2E2C37),
    outline = Color(0xFF4A4858),
    outlineVariant = Color(0xFF34323F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Colours Material 3 has no role for: answers, the streak and inline code. */
@Immutable
data class ExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val onStreakContainer: Color,
    val codeBackground: Color,
    val codeText: Color,
)

internal val LightExtra = ExtraColors(
    success = Color(0xFF1E7A45),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD3F4DD),
    onSuccessContainer = Color(0xFF0B3D20),
    streak = Color(0xFFF07A2B),
    streakContainer = Color(0xFFFFE6D2),
    onStreakContainer = Color(0xFF4A1D00),
    codeBackground = Color(0xFFE7E3FA),
    codeText = Color(0xFF2B1FA0),
)

internal val DarkExtra = ExtraColors(
    success = Color(0xFF7FD8A0),
    onSuccess = Color(0xFF0B3D20),
    successContainer = Color(0xFF154D2E),
    onSuccessContainer = Color(0xFFC9F2D6),
    streak = Color(0xFFFFB37A),
    streakContainer = Color(0xFF5A2A08),
    onStreakContainer = Color(0xFFFFE6D2),
    codeBackground = Color(0xFF2A2640),
    codeText = Color(0xFFCFC7FF),
)
