package uz.devsuhbat

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.devsuhbat.ui.DevSuhbatNavHost
import uz.devsuhbat.ui.Routes
import uz.devsuhbat.ui.theme.DevSuhbatTheme
import uz.devsuhbat.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DevSuhbatApp).container

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Nothing is drawn until the stored settings arrive; the window background covers the gap.
            val current = settings ?: return@setContent
            val dark = isDark(current.theme)

            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            // Decided once: finishing onboarding navigates by itself and must not rebuild the graph.
            val startDestination = remember {
                val ready = current.onboardingDone && current.fieldId != null && current.level != null
                if (ready) Routes.HOME else Routes.ONBOARDING
            }

            DevSuhbatTheme(dark = dark) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DevSuhbatNavHost(container = container, startDestination = startDestination)
                }
            }
        }
    }
}
