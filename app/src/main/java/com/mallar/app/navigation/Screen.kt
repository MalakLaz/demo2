package com.mallar.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object ARDetection : Screen("ar_detection")
    object SearchDestination : Screen("search_destination/{storeId}") {
        fun createRoute(storeId: String = "") = "search_destination/$storeId"
    }
    object ARNavigation : Screen("ar_navigation/{storeId}") {
        fun createRoute(storeId: String) = "ar_navigation/$storeId"
    }
    object Arrival : Screen("arrival/{storeName}") {
        fun createRoute(storeName: String) = "arrival/$storeName"
    }
}
