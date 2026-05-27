package com.example.flightbooking.data.models

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Enhanced flight data models for comprehensive booking system
 */

/**
 * Airline information
 */
data class Airline(
    val code: String,
    val name: String,
    val logo: String? = null,
    val rating: Float? = null
)

/**
 * Airport information with coordinates
 */
data class Airport(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "UTC"
)

/**
 * Flight segment (one leg of a journey)
 */
data class FlightSegment(
    val flightNumber: String,
    val airline: Airline,
    val origin: Airport,
    val destination: Airport,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val duration: Int, // in minutes
    val aircraft: String = "",
    val cabinClass: CabinClass = CabinClass.ECONOMY,
    val departureTerminal: String? = null,
    val departureGate: String? = null,
    val arrivalTerminal: String? = null,
    val arrivalGate: String? = null
)

/**
 * Complete flight option (may include multiple segments for connections)
 */
data class Flight(
    val id: String,
    val flightNumber: String,
    val airline: Airline,
    val segments: List<FlightSegment>,
    val price: Double,
    val currency: String = "USD",
    val availableSeats: Int,
    val baggageInfo: BaggageInfo,
    val amenities: List<String> = emptyList(),
    val isRefundable: Boolean = false,
    val carbonEmissions: Double = 0.0,
    val cabinClass: CabinClass = CabinClass.ECONOMY,
    val totalDuration: Int // in minutes
) {
    /**
     * Get total duration as java.time.Duration
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getTotalDuration(): java.time.Duration {
        return java.time.Duration.ofMinutes(totalDuration.toLong())
    }
    
    /**
     * Get number of stops
     */
    fun getStops(): Int = segments.size - 1
    
    /**
     * Get departure time
     */
    fun getDepartureTime(): LocalDateTime = segments.first().departureTime
    
    /**
     * Get arrival time
     */
    fun getArrivalTime(): LocalDateTime = segments.last().arrivalTime
    
    /**
     * Get all airlines involved in this flight
     */
    fun getAirlines(): List<Airline> = segments.map { it.airline }.distinct()
}

/**
 * Baggage allowance information
 */
data class BaggageInfo(
    val carryOnAllowed: Int = 1,
    val carryOnWeight: String = "7 kg",
    val checkedBagsIncluded: Int = 1,
    val checkedBagWeight: String = "23 kg",
    val additionalBagFee: Double? = null
)

/**
 * Flight search filters
 */
data class FlightFilters(
    val minPrice: Double = 0.0,
    val maxPrice: Double = 5000.0,
    val airlines: Set<String> = emptySet(),
    val maxStops: Int? = null,
    val departureTimeRanges: Set<String> = emptySet(), // "morning", "afternoon", "evening", "night"
    val maxDuration: Int? = null, // in minutes
    val cabinClasses: Set<CabinClass> = emptySet(),
    val refundableOnly: Boolean = false
)

/**
 * Sort options for flight results
 */
enum class FlightSortOption {
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    DURATION_SHORTEST,
    DURATION_LONGEST,
    DEPARTURE_EARLIEST,
    DEPARTURE_LATEST,
    ARRIVAL_EARLIEST,
    ARRIVAL_LATEST,
    BEST_VALUE
}

/**
 * Passenger information for booking
 */
data class PassengerInfo(
    val title: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: String = "",
    val nationality: String = "",
    val passportNumber: String = "",
    val passportExpiry: LocalDate? = null,
    val frequentFlyerNumber: String = "",
    val specialRequests: String = "",
    val seatPreference: String? = null,
    val mealPreference: String? = null,
    val specialAssistance: List<String> = emptyList()
)

/**
 * Contact information
 */
data class ContactInfo(
    val email: String = "",
    val confirmEmail: String = "",
    val phone: String = "",
    val countryCode: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val subscribeToNewsletter: Boolean = false
)

/**
 * Payment information
 */
data class PaymentInfo(
    val paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
    val cardNumber: String = "",
    val cardHolderName: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val saveCard: Boolean = false,
    val paypalEmail: String = "",
    val bankAccountNumber: String = "",
    val bankRoutingNumber: String = "",
    val bankName: String = "",
    val cryptoWalletAddress: String = "",
    val billingAddress: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingZipCode: String = "",
    val billingCountry: String = "",
    val amount: Double = 0.0
)

/**
 * Payment methods
 */
enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    BANK_TRANSFER,
    CRYPTO
}

/**
 * Flight search results state
 */
data class FlightSearchResults(
    val flights: List<Flight> = emptyList(),
    val filters: FlightFilters = FlightFilters(),
    val sortOption: FlightSortOption = FlightSortOption.BEST_VALUE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFlightIds: Set<String> = emptySet() // For comparison
)

/**
 * Booking information
 */
data class Booking(
    val id: String,
    val bookingReference: String,
    val flight: Flight,
    val passengers: List<PassengerInfo>,
    val contactInfo: ContactInfo,
    val paymentInfo: PaymentInfo,
    val bookingDate: LocalDateTime,
    val status: BookingStatus,
    val totalPrice: Double
)

/**
 * Booking status
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CANCELLED,
    COMPLETED
}

// Made with Bob
