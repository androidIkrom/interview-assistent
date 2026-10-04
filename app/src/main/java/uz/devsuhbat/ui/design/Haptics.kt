package uz.devsuhbat.ui.design

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class HapticEvent { SELECT, CORRECT, WRONG }

/** CONFIRM and REJECT exist from Android 11; older versions get the closest older constant. */
fun hapticConstant(event: HapticEvent, sdkInt: Int): Int = when (event) {
    HapticEvent.SELECT -> HapticFeedbackConstants.CLOCK_TICK
    HapticEvent.CORRECT ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    HapticEvent.WRONG ->
        if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
}

@Composable
fun rememberHaptics(): (HapticEvent) -> Unit {
    val view = LocalView.current
    return remember(view) { { event -> view.performHapticFeedback(hapticConstant(event, Build.VERSION.SDK_INT)) } }
}
