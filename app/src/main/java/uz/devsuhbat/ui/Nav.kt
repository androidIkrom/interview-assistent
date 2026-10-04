package uz.devsuhbat.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.devsuhbat.AppContainer
import uz.devsuhbat.R
import uz.devsuhbat.ui.design.DsMotion
import uz.devsuhbat.ui.home.HomeScreen
import uz.devsuhbat.ui.mock.MockScreen
import uz.devsuhbat.ui.onboarding.OnboardingScreen
import uz.devsuhbat.ui.session.SessionScreen
import uz.devsuhbat.ui.settings.SettingsScreen
import uz.devsuhbat.ui.topics.TopicsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TOPICS = "topics"
    const val SETTINGS = "settings"
    const val MOCK = "mock"

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

private class Tab(val route: String, val icon: ImageVector, val label: Int)

private val tabs = listOf(
    Tab(Routes.HOME, Icons.Rounded.Home, R.string.nav_home),
    Tab(Routes.TOPICS, Icons.AutoMirrored.Rounded.List, R.string.nav_topics),
)

/** Switches tabs the standard way: one copy of each tab, each keeping its own state. */
private fun NavController.openTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.motion() =
    navMotion(initialState.destination.route, targetState.destination.route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(forward: Boolean): EnterTransition =
    when (motion()) {
        NavMotion.FADE -> fadeIn(DsMotion.effectsDefault())
        NavMotion.SLIDE -> slideInHorizontally(DsMotion.spatialDefault()) { if (forward) it / 4 else -it / 4 } +
            fadeIn(DsMotion.effectsDefault())
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition =
    when (motion()) {
        NavMotion.FADE -> fadeOut(DsMotion.effectsDefault())
        NavMotion.SLIDE -> slideOutHorizontally(DsMotion.spatialDefault()) { if (forward) -it / 4 else it / 4 } +
            fadeOut(DsMotion.effectsDefault())
    }

/**
 * Keeps room for the bottom bar under a tab. The inner screen has its own Scaffold, so the space is also marked as
 * consumed, or the screen would pad the navigation bar inset a second time.
 */
@Composable
private fun BarRoom(route: String, barHeight: Dp, content: @Composable () -> Unit) {
    val bottom = PaddingValues(bottom = contentBottomPadding(route, barHeight))
    Box(Modifier.padding(bottom).consumeWindowInsets(bottom)) { content() }
}

@Composable
fun DevSuhbatNavHost(container: AppContainer, startDestination: String) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevel(route),
                enter = slideInVertically(DsMotion.spatialDefault()) { it },
                exit = slideOutVertically(DsMotion.spatialDefault()) { it },
            ) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { if (route != tab.route) nav.openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val barHeight = padding.calculateBottomPadding()
        NavHost(
            navController = nav,
            startDestination = startDestination,
            enterTransition = { enter(forward = true) },
            exitTransition = { exit(forward = true) },
            popEnterTransition = { enter(forward = false) },
            popExitTransition = { exit(forward = false) },
        ) {
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
                BarRoom(Routes.HOME, barHeight) {
                    HomeScreen(
                        container = container,
                        onPractice = { nav.navigate(Routes.session(Routes.MIXED)) },
                        onMistakes = { nav.navigate(Routes.session(Routes.MISTAKES)) },
                        onMock = { nav.navigate(Routes.MOCK) },
                        onTopic = { topicId -> nav.navigate(Routes.session(topicId)) },
                        onAllTopics = { nav.openTab(Routes.TOPICS) },
                        onSettings = { nav.navigate(Routes.SETTINGS) },
                    )
                }
            }
            composable(Routes.TOPICS) {
                BarRoom(Routes.TOPICS, barHeight) {
                    TopicsScreen(
                        container = container,
                        onOpen = { topicId -> nav.navigate(Routes.session(topicId)) },
                    )
                }
            }
            composable(Routes.MOCK) {
                MockScreen(
                    container = container,
                    onExit = { nav.popBackStack() },
                    // The mock leaves the back stack, so "back" from the mistakes session lands on Home.
                    onMistakes = { nav.navigate(Routes.session(Routes.MISTAKES)) { popUpTo(Routes.HOME) } },
                    onHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
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
}
