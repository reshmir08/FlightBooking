package com.example.flightbooking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightbooking.data.MockData
import com.example.flightbooking.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * ViewModel for flight search and results
 */
class FlightSearchViewModel : ViewModel() {
    
    private val _searchResults = MutableStateFlow(FlightSearchResults())
    val searchResults: StateFlow<FlightSearchResults> = _searchResults.asStateFlow()
    
    /**
     * Search for flights
     */
    fun searchFlights(
        from: Airport,
        to: Airport,
        departureDate: LocalDateTime,
        returnDate: LocalDateTime? = null,
        passengers: Int = 1,
        cabinClass: CabinClass = CabinClass.ECONOMY
    ) {
        viewModelScope.launch {
            _searchResults.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Simulate API call delay
                kotlinx.coroutines.delay(1000)
                
                // Get mock flights
                val flights = MockData.generateSampleFlights(from, to, departureDate)
                
                _searchResults.update {
                    it.copy(
                        flights = flights,
                        isLoading = false
                    )
                }
                
                // Apply current filters and sort
                applyFiltersAndSort()
                
            } catch (e: Exception) {
                _searchResults.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to search flights"
                    )
                }
            }
        }
    }
    
    /**
     * Update filters
     */
    fun updateFilters(filters: FlightFilters) {
        _searchResults.update { it.copy(filters = filters) }
        applyFiltersAndSort()
    }
    
    /**
     * Update sort option
     */
    fun updateSortOption(sortOption: FlightSortOption) {
        _searchResults.update { it.copy(sortOption = sortOption) }
        applyFiltersAndSort()
    }
    
    /**
     * Toggle flight selection for comparison
     */
    fun toggleFlightSelection(flightId: String) {
        _searchResults.update { currentState ->
            val selectedIds = currentState.selectedFlightIds.toMutableSet()
            if (selectedIds.contains(flightId)) {
                selectedIds.remove(flightId)
            } else {
                if (selectedIds.size < 3) {
                    selectedIds.add(flightId)
                }
            }
            currentState.copy(selectedFlightIds = selectedIds)
        }
    }
    
    /**
     * Clear all selections
     */
    fun clearSelections() {
        _searchResults.update { it.copy(selectedFlightIds = emptySet()) }
    }
    
    /**
     * Apply filters and sorting to flight results
     */
    private fun applyFiltersAndSort() {
        _searchResults.update { currentState ->
            var filteredFlights = currentState.flights
            val filters = currentState.filters
            
            // Apply price filter
            filteredFlights = filteredFlights.filter {
                it.price >= filters.minPrice && it.price <= filters.maxPrice
            }
            
            // Apply airline filter
            if (filters.airlines.isNotEmpty()) {
                filteredFlights = filteredFlights.filter { flight ->
                    flight.getAirlines().any { airline ->
                        filters.airlines.contains(airline.code)
                    }
                }
            }
            
            // Apply stops filter
            if (filters.maxStops != null) {
                filteredFlights = filteredFlights.filter { it.getStops() <= filters.maxStops }
            }
            
            // Apply cabin class filter
            if (filters.cabinClasses.isNotEmpty()) {
                filteredFlights = filteredFlights.filter { flight ->
                    flight.segments.any { segment ->
                        filters.cabinClasses.contains(segment.cabinClass)
                    }
                }
            }
            
            // Apply refundable filter
            if (filters.refundableOnly) {
                filteredFlights = filteredFlights.filter { it.isRefundable }
            }
            
            // Apply duration filter
            if (filters.maxDuration != null) {
                filteredFlights = filteredFlights.filter {
                    it.totalDuration <= filters.maxDuration
                }
            }
            
            // Apply sorting
            filteredFlights = when (currentState.sortOption) {
                FlightSortOption.PRICE_LOW_TO_HIGH -> filteredFlights.sortedBy { it.price }
                FlightSortOption.PRICE_HIGH_TO_LOW -> filteredFlights.sortedByDescending { it.price }
                FlightSortOption.DURATION_SHORTEST -> filteredFlights.sortedBy { it.totalDuration }
                FlightSortOption.DURATION_LONGEST -> filteredFlights.sortedByDescending { it.totalDuration }
                FlightSortOption.DEPARTURE_EARLIEST -> filteredFlights.sortedBy { it.getDepartureTime() }
                FlightSortOption.DEPARTURE_LATEST -> filteredFlights.sortedByDescending { it.getDepartureTime() }
                FlightSortOption.ARRIVAL_EARLIEST -> filteredFlights.sortedBy { it.getArrivalTime() }
                FlightSortOption.ARRIVAL_LATEST -> filteredFlights.sortedByDescending { it.getArrivalTime() }
                FlightSortOption.BEST_VALUE -> {
                    // Best value: combination of price and duration
                    filteredFlights.sortedBy {
                        (it.price / 100) + (it.totalDuration / 60.0)
                    }
                }
            }
            
            currentState.copy(flights = filteredFlights)
        }
    }
    
    /**
     * Get selected flights for comparison
     */
    fun getSelectedFlights(): List<Flight> {
        val currentState = _searchResults.value
        return currentState.flights.filter { 
            currentState.selectedFlightIds.contains(it.id) 
        }
    }
}

// Made with Bob
