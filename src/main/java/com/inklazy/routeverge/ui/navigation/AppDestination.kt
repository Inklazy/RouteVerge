package com.inklazy.routeverge.ui.navigation

/**
 * Top-level destinations of RouteVerge.
 *
 * The app intentionally keeps the lightweight enum + AnimatedContent
 * navigation (3 destinations, no back stack) instead of pulling in a
 * navigation library. AppRoot decides which screen is shown; screens
 * never decide navigation themselves.
 */
enum class AppDestination {
    HOME,
    SETTINGS,
    POINT_PICKER,
    ROUTE_EDITOR
}
