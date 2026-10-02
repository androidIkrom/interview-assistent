package uz.devsuhbat.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.devsuhbat.AppContainer
import uz.devsuhbat.ui.home.HomeScreen
import uz.devsuhbat.ui.onboarding.OnboardingScreen
import uz.devsuhbat.ui.session.SessionScreen
import uz.devsuhbat.ui.settings.SettingsScreen
import uz.devsuhbat.ui.topics.TopicsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TOPICS = "topics"
    const val SETTINGS = "settings"

    /** The onboarding picker opened from Settings to change field and level. */
    const val PROFILE = "profile"

    const val TOPIC_ID = "topicId"
    const val SESSION = "session/{$TOPIC_ID}"

    /** Topic id for a session mixing all topics of the field. */
    const val MIXED = "mixed"

    /** Topic id for a session over the questions that are due for repetition. */
    const val MISTAKES = "mistakes"

    fun session(topicId: String) = "session/$topicId"
}

@Composable
fun DevSuhbatNavHost(container: AppContainer, startDestination: String) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                container = container,
                onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
                onBack = null,
            )
        }
        composable(Routes.PROFILE) {
            OnboardingScreen(
                container = container,
                onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onPractice = { nav.navigate(Routes.TOPICS) },
                onMistakes = { nav.navigate(Routes.session(Routes.MISTAKES)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.TOPICS) {
            TopicsScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onOpen = { topicId -> nav.navigate(Routes.session(topicId)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onEditProfile = { nav.navigate(Routes.PROFILE) },
            )
        }
        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument(Routes.TOPIC_ID) { type = NavType.StringType }),
        ) { entry ->
            SessionScreen(
                container = container,
                topicId = entry.arguments?.getString(Routes.TOPIC_ID) ?: Routes.MIXED,
                onExit = { nav.popBackStack() },
                // Back to where the session was opened from: the topic list, or Home for a mistakes session.
                onAgain = {
                    if (!nav.popBackStack(Routes.TOPICS, inclusive = false)) {
                        nav.popBackStack(Routes.HOME, inclusive = false)
                    }
                },
                onHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
