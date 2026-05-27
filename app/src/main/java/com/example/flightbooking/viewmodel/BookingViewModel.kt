package com.example.flightbooking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightbooking.data.models.BookingState
import com.example.flightbooking.data.models.CabinClass
import com.example.flightbooking.data.models.FlightSearchCriteria
import com.example.flightbooking.data.models.Location
import com.example.flightbooking.data.models.TripType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for managing flight booking state
 */
class BookingViewModel : ViewModel() {
    
    private val _bookingState = MutableStateFlow(BookingState())
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()
    
    /**
     * Update trip type
     */
    fun updateTripType(tripType: TripType) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    tripType = tripType,
                    // Clear return date if switching to one-way
                    returnDate = if (tripType == TripType.ONE_WAY) null 
                                else currentState.searchCriteria.returnDate
                )
            )
        }
    }
    
    /**
     * Update departure location
     */
    fun updateDeparture(location: Location) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    departure = location
                )
            )
        }
    }
    
    /**
     * Update arrival location
     */
    fun updateArrival(location: Location) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    arrival = location
                )
            )
        }
    }
    
    /**
     * Swap departure and arrival locations
     */
    fun swapLocations() {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    departure = currentState.searchCriteria.arrival,
                    arrival = currentState.searchCriteria.departure
                )
            )
        }
    }
    
    /**
     * Update departure date
     */
    fun updateDepartureDate(date: LocalDate) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    departureDate = date
                )
            )
        }
    }
    
    /**
     * Update return date
     */
    fun updateReturnDate(date: LocalDate?) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    returnDate = date
                )
            )
        }
    }
    
    /**
     * Update number of passengers
     */
    fun updatePassengers(count: Int) {
        if (count in 1..9) {
            _bookingState.update { currentState ->
                currentState.copy(
                    searchCriteria = currentState.searchCriteria.copy(
                        passengers = count
                    )
                )
            }
        }
    }
    
    /**
     * Update cabin class
     */
    fun updateCabinClass(cabinClass: CabinClass) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    cabinClass = cabinClass
                )
            )
        }
    }
    
    /**
     * Toggle Aeroplan points usage
     */
    fun toggleAeroplanPoints() {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    useAeroplanPoints = !currentState.searchCriteria.useAeroplanPoints
                )
            )
        }
    }
    
    /**
     * Update promotion code
     */
    fun updatePromotionCode(code: String) {
        _bookingState.update { currentState ->
            currentState.copy(
                searchCriteria = currentState.searchCriteria.copy(
                    promotionCode = code
                )
            )
        }
    }
    
    /**
     * Search for flights
     */
    fun searchFlights() {
        viewModelScope.launch {
            _bookingState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Validate search criteria
                val criteria = _bookingState.value.searchCriteria
                
                if (criteria.departure.isEmpty()) {
                    throw IllegalStateException("Please select a departure location")
                }
                
                if (criteria.arrival.isEmpty()) {
                    throw IllegalStateException("Please select an arrival location")
                }
                
                if (criteria.departureDate == null) {
                    throw IllegalStateException("Please select a departure date")
                }
                
                if (criteria.tripType == TripType.ROUND_TRIP && criteria.returnDate == null) {
                    throw IllegalStateException("Please select a return date")
                }
                
                // TODO: Implement actual flight search logic
                // For now, just simulate a successful search
                
                _bookingState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                _bookingState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _bookingState.update { it.copy(error = null) }
    }
}

// Made with Bob
