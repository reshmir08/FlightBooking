package com.example.flightbooking.data.models

import java.time.LocalDate

/**
 * Enum representing the type of trip
 */
enum class TripType {
    ROUND_TRIP,
    ONE_WAY,
    MULTI_CITY
}

/**
 * Enum representing cabin classes
 */
enum class CabinClass(val displayName: String) {
    ECONOMY("Economy"),
    PREMIUM_ECONOMY("Premium Economy"),
    BUSINESS("Business"),
    FIRST_CLASS("First Class")
}

/**
 * Data class representing a location (airport)
 */
data class Location(
    val code: String = "",
    val name: String = "",
    val city: String = ""
) {
    fun isEmpty(): Boolean = code.isEmpty() && name.isEmpty()
    
    fun getDisplayText(): String {
        return if (isEmpty()) "" else "$city ($code)"
    }
}

/**
 * Data class representing flight search criteria
 */
data class FlightSearchCriteria(
    val tripType: TripType = TripType.ROUND_TRIP,
    val departure: Location = Location(),
    val arrival: Location = Location(),
    val departureDate: LocalDate? = null,
    val returnDate: LocalDate? = null,
    val passengers: Int = 1,
    val cabinClass: CabinClass = CabinClass.ECONOMY,
    val useAeroplanPoints: Boolean = false,
    val promotionCode: String = ""
)

/**
 * Data class representing the booking state
 */
data class BookingState(
    val searchCriteria: FlightSearchCriteria = FlightSearchCriteria(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDatePicker: Boolean = false,
    val showPassengerPicker: Boolean = false,
    val showCabinPicker: Boolean = false,
    val showDeparturePicker: Boolean = false,
    val showArrivalPicker: Boolean = false
)

/**
 * Bottom navigation items
 */
enum class BottomNavItem(val title: String, val route: String) {
    BOOK("Book", "book"),
    AIRPORT("Airport", "airport"),
    PROFILE("Profile", "profile"),
    SETTINGS("Settings", "settings")
}

/**
 * Data class for passenger selection
 */
data class PassengerSelection(
    val adults: Int = 1,
    val children: Int = 0
) {
    fun getTotalPassengers(): Int = adults + children
    
    fun getDisplayText(): String {
        val parts = mutableListOf<String>()
        if (adults > 0) parts.add("$adults Adult${if (adults > 1) "s" else ""}")
        if (children > 0) parts.add("$children Child${if (children > 1) "ren" else ""}")
        return parts.joinToString(", ")
    }
}

// Made with Bob
