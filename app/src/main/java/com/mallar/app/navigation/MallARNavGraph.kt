package com.mallar.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mallar.app.ui.screens.*
import com.mallar.app.viewmodel.MallARViewModel

@Composable
fun MallARNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val viewModel: MallARViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(
                stores = uiState.allStores,
                onOpenCamera = {
                    viewModel.clearDetection()
                    navController.navigate(Screen.ARDetection.route)
                },
                onSearchDestination = {
                    navController.navigate(Screen.SearchDestination.createRoute())
                }
            )
        }

        // AR Detection Screen
        composable(Screen.ARDetection.route) {
            ARDetectionScreen(
                isDetecting        = uiState.isDetecting,
                detectionProgress  = uiState.detectionProgress,
                detectedLocation   = uiState.detectedLocation,
                detectionError     = uiState.detectionError,
                // Real path: camera frame → TFLite model
                onScanFrame        = { bitmap -> viewModel.runARDetection(bitmap) },
                // Fallback path: no frame available
                onStartDetection   = { viewModel.startARDetection() },
                onLocationConfirmed = {
                    navController.navigate(Screen.SearchDestination.createRoute()) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Search Destination Screen
        composable(
            route = Screen.SearchDestination.route,
            arguments = listOf(navArgument("storeId") {
                type = NavType.StringType; defaultValue = ""
            })
        ) { backStackEntry ->
            val preselectedId = backStackEntry.arguments?.getString("storeId") ?: ""
            LaunchedEffect(preselectedId) {
                if (preselectedId.isNotEmpty()) viewModel.selectStoreById(preselectedId)
            }
            SearchDestinationScreen(
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onStoreSelected = { storeId ->
                    viewModel.selectStoreById(storeId)
                    navController.navigate(Screen.ARNavigation.createRoute(storeId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // AR Navigation Screen
        composable(
            route = Screen.ARNavigation.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            LaunchedEffect(storeId) {
                if (storeId.isNotEmpty()) viewModel.selectStoreById(storeId)
            }

            // Navigate to arrival when last step reached
            val hasArrived = uiState.hasArrived
            val storeName = uiState.selectedStore?.name ?: "Store"
            LaunchedEffect(hasArrived) {
                if (hasArrived) {
                    navController.navigate(Screen.Arrival.createRoute(storeName)) {
                        popUpTo(Screen.ARNavigation.route) { inclusive = true }
                    }
                }
            }

            ARNavigationScreen(
                store = uiState.selectedStore,
                navigationSteps = uiState.navigationSteps,
                currentStepIndex = uiState.currentNavigationStep,
                onNextStep = { viewModel.nextNavigationStep() },
                onPreviousStep = { viewModel.previousNavigationStep() },
                onStepDetected = { viewModel.onStepDetected() },
                onBack = {
                    viewModel.resetNavigation()
                    navController.navigateUp()
                }
            )
        }

        // Arrival Screen
        composable(
            route = Screen.Arrival.route,
            arguments = listOf(navArgument("storeName") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeName = backStackEntry.arguments?.getString("storeName") ?: "Your Destination"
            ArrivalScreen(
                storeName = storeName,
                onNavigateHome = {
                    viewModel.resetNavigation()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
