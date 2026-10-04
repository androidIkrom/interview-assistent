package uz.devsuhbat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import uz.devsuhbat.data.ThemeMode
import uz.devsuhbat.ui.design.LocalReducedMotion
import uz.devsuhbat.ui.design.rememberReducedMotion

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DevSuhbatTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtraColors provides if (dark) DarkExtra else LightExtra,
        LocalReducedMotion provides rememberReducedMotion(),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = DevSuhbatTypography,
            shapes = DevSuhbatShapes,
            content = content,
        )
    }
}
