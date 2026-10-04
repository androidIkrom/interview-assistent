package uz.devsuhbat.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Destinations reachable from the bottom navigation bar, in bar order. */
val TOP_LEVEL_ROUTES: List<String> = listOf(Routes.HOME, Routes.TOPICS, Routes.STATS)

fun isTopLevel(route: String?): Boolean = route in TOP_LEVEL_ROUTES

/**
 * Room a destination keeps under itself for the bottom bar. Only tabs keep it: a screen above them takes the full
 * height at once instead of jumping when the sliding bar finally lets go of its space.
 */
fun contentBottomPadding(route: String?, barHeight: Dp): Dp = if (isTopLevel(route)) barHeight else 0.dp

/** Where "Yana mashq" and Back lead from a finished session: the tab it was opened from, otherwise Home. */
fun sessionReturnRoute(previousRoute: String?): String =
    if (previousRoute != null && isTopLevel(previousRoute)) previousRoute else Routes.HOME

enum class NavMotion { FADE, SLIDE }

/** Tabs cross-fade into each other; any screen opened above them slides in from the side. */
fun navMotion(from: String?, to: String?): NavMotion =
    if (isTopLevel(from) && isTopLevel(to)) NavMotion.FADE else NavMotion.SLIDE
