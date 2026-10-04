package uz.devsuhbat.ui

/** Destinations reachable from the bottom navigation bar, in bar order. */
val TOP_LEVEL_ROUTES: List<String> = listOf(Routes.HOME, Routes.TOPICS)

fun isTopLevel(route: String?): Boolean = route in TOP_LEVEL_ROUTES

enum class NavMotion { FADE, SLIDE }

/** Tabs cross-fade into each other; any screen opened above them slides in from the side. */
fun navMotion(from: String?, to: String?): NavMotion =
    if (isTopLevel(from) && isTopLevel(to)) NavMotion.FADE else NavMotion.SLIDE
