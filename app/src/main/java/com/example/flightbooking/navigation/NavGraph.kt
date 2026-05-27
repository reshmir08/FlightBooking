package com.example.flightbooking.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.flightbooking.data.models.Airport
import com.example.flightbooking.ui.screens.*
import com.example.flightbooking.viewmodel.BookingViewModel
import com.example.flightbooking.viewmodel.FlightSearchViewModel
import java.time.LocalDateTime

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Book : Screen("book")
    object Airport : Screen("airport")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object FlightResults : Screen("flight_results")
    object FlightDetails : Screen("flight_details/{flightId}") {
        fun createRoute(flightId: String) = "flight_details/$flightId"
    }
    object PassengerForm : Screen("passenger_form/{flightId}") {
        fun createRoute(flightId: String) = "passenger_form/$flightId"
    }
    object Payment : Screen("payment/{flightId}") {
        fun createRoute(flightId: String) = "payment/$flightId"
    }
    object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }
    object TerminalMap : Screen("terminal_map")
    object GateFinder : Screen("gate_finder")
    object AmenitiesList : Screen("amenities_list")
    object Navigation : Screen("navigation")
}

/**
 * Main navigation graph
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Book.route
) {
    // Shared ViewModels
    val bookingViewModel: BookingViewModel = viewModel()
    val flightSearchViewModel: FlightSearchViewModel = viewModel()
    val airportNavigationViewModel: com.example.flightbooking.viewmodel.AirportNavigationViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Book Screen (Main booking interface)
        composable(Screen.Book.route) {
            BookingScreen(
                viewModel = bookingViewModel,
                onSearchFlights = {
                    // Get search criteria from booking view model
                    val criteria = bookingViewModel.bookingState.value.searchCriteria
                    
                    // Convert Location to Airport
                    val fromAirport = Airport(
                        code = criteria.departure.code,
                        name = criteria.departure.name,
                        city = criteria.departure.city,
                        country = ""
                    )
                    
                    val toAirport = Airport(
                        code = criteria.arrival.code,
                        name = criteria.arrival.name,
                        city = criteria.arrival.city,
                        country = ""
                    )
                    
                    // Convert LocalDate to LocalDateTime (set time to 00:00)
                    val departureDateTime = criteria.departureDate?.atStartOfDay()
                    val returnDateTime = criteria.returnDate?.atStartOfDay()
                    
                    // Trigger flight search
                    if (departureDateTime != null) {
                        flightSearchViewModel.searchFlights(
                            from = fromAirport,
                            to = toAirport,
                            departureDate = departureDateTime,
                            returnDate = returnDateTime,
                            passengers = criteria.passengers,
                            cabinClass = criteria.cabinClass
                        )
                        
                        // Navigate to results
                        navController.navigate(Screen.FlightResults.route)
                    }
                },
                onNavigate = { navItem ->
                    navController.navigate(navItem.route)
                }
            )
        }

        // Airport Hub Screen
        composable(Screen.Airport.route) {
            AirportHubScreen(
                onNavigateToTerminalMap = {
                    navController.navigate(Screen.TerminalMap.route)
                },
                onNavigateToGateFinder = {
                    navController.navigate(Screen.GateFinder.route)
                },
                onNavigateToAmenities = {
                    navController.navigate(Screen.AmenitiesList.route)
                },
                onNavigateToNavigation = {
                    navController.navigate(Screen.Navigation.route)
                },
                onNavigate = { navItem ->
                    navController.navigate(navItem.route)
                }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigate = { navItem ->
                    navController.navigate(navItem.route)
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigate = { navItem ->
                    navController.navigate(navItem.route)
                }
            )
        }

        // Flight Results Screen
        composable(Screen.FlightResults.route) {
            FlightResultsScreen(
                viewModel = flightSearchViewModel,
                onFlightClick = { flight ->
                    navController.navigate(Screen.FlightDetails.createRoute(flight.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Flight Details Screen
        composable(
            route = Screen.FlightDetails.route,
            arguments = listOf(
                navArgument("flightId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getString("flightId") ?: ""
            // Get flight from mock data
            val flight = com.example.flightbooking.data.MockData.generateSampleFlights().first()
            FlightDetailsScreen(
                flight = flight,
                onBookClick = {
                    navController.navigate(Screen.PassengerForm.createRoute(flightId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Passenger Form Screen
        composable(
            route = Screen.PassengerForm.route,
            arguments = listOf(
                navArgument("flightId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getString("flightId") ?: ""
            PassengerFormScreen(
                passengerCount = 1,
                onContinue = { passengers, contactInfo ->
                    navController.navigate(Screen.Payment.createRoute(flightId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Payment Screen
        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("flightId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getString("flightId") ?: ""
            // Get flight from mock data
            val flight = com.example.flightbooking.data.MockData.generateSampleFlights().first()
            PaymentScreen(
                flight = flight,
                totalPrice = 500.0,
                onConfirmPayment = { paymentInfo ->
                    val bookingId = "BK${System.currentTimeMillis()}"
                    navController.navigate(Screen.BookingConfirmation.createRoute(bookingId)) {
                        popUpTo(Screen.Book.route) {
                            inclusive = false
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Booking Confirmation Screen
        composable(
            route = Screen.BookingConfirmation.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            // Create a mock booking for demo
            val mockBooking = com.example.flightbooking.data.models.Booking(
                id = bookingId,
                bookingReference = "ABC123",
                flight = com.example.flightbooking.data.MockData.generateSampleFlights().first(),
                passengers = emptyList(),
                contactInfo = com.example.flightbooking.data.models.ContactInfo(
                    email = "user@example.com",
                    phone = "+1234567890",
                    countryCode = "+1"
                ),
                paymentInfo = com.example.flightbooking.data.models.PaymentInfo(
                    paymentMethod = com.example.flightbooking.data.models.PaymentMethod.CREDIT_CARD,
                    cardNumber = "****1234",
                    cardHolderName = "John Doe",
                    expiryDate = "12/25"
                ),
                bookingDate = java.time.LocalDateTime.now(),
                status = com.example.flightbooking.data.models.BookingStatus.CONFIRMED,
                totalPrice = 500.0
            )
            BookingConfirmationScreen(
                booking = mockBooking,
                onDoneClick = {
                    navController.navigate(Screen.Book.route) {
                        popUpTo(Screen.Book.route) {
                            inclusive = true
                        }
                    }
                },
                onViewBookingClick = {
                    // Could navigate to a booking details screen
                },
                onDownloadTicketClick = {
                    // Could download ticket
                }
            )
        }

        // Terminal Map Screen
        composable(Screen.TerminalMap.route) {
            TerminalMapScreen(
                viewModel = airportNavigationViewModel,
                onAmenityClick = { amenity ->
                    navController.navigate(Screen.AmenitiesList.route)
                },
                onGateClick = { gate ->
                    navController.navigate(Screen.GateFinder.route)
                }
            )
        }

        // Gate Finder Screen
        composable(Screen.GateFinder.route) {
            GateFinderScreen(
                onNavigateToGate = { gate ->
                    // Set up navigation in the ViewModel
                    airportNavigationViewModel.navigateToGate(gate.number)
                    // Navigate to the Navigation screen
                    navController.navigate(Screen.Navigation.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Amenities List Screen
        composable(Screen.AmenitiesList.route) {
            AmenitiesListScreen(
                viewModel = airportNavigationViewModel,
                onAmenityClick = { _ ->
                    // Could show amenity details
                },
                onNavigateClick = { _ ->
                    navController.navigate(Screen.Navigation.route)
                }
            )
        }

        // Navigation Screen
        composable(Screen.Navigation.route) {
            NavigationScreen(
                viewModel = airportNavigationViewModel,
                onStopNavigation = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Made with Bob