package com.pillion.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillion.di.AppContainer
import com.pillion.ui.livetrip.LiveTripRoute
import com.pillion.ui.livetrip.LiveTripViewModel
import com.pillion.ui.tripbuilder.TripBuilderRoute
import com.pillion.ui.tripbuilder.TripBuilderViewModel
import com.pillion.ui.triplist.TripListRoute
import com.pillion.ui.triplist.TripListViewModel

@Composable
fun PillionNavHost(
    container: AppContainer,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.TripList,
    ) {
        composable(Routes.TripList) {
            val viewModel: TripListViewModel = viewModel(factory = TripListViewModel.Factory(container))
            TripListRoute(
                viewModel = viewModel,
                onCreateTrip = {
                    navController.navigate(Routes.tripBuilder(0L))
                },
                onOpenTrip = { tripId ->
                    navController.navigate(Routes.tripBuilder(tripId))
                },
                onStartTrip = { tripId ->
                    navController.navigate(Routes.liveTrip(tripId))
                },
            )
        }

        composable(
            route = Routes.TripBuilderPattern,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
            val viewModel: TripBuilderViewModel = viewModel(
                factory = TripBuilderViewModel.Factory(
                    appContainer = container,
                    tripId = tripId,
                )
            )

            TripBuilderRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.LiveTripPattern,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
            val viewModel: LiveTripViewModel = viewModel(
                factory = LiveTripViewModel.Factory(
                    appContainer = container,
                    tripId = tripId,
                )
            )

            LiveTripRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
