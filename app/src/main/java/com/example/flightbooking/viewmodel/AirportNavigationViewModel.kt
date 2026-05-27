package com.example.flightbooking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightbooking.data.models.*
import com.example.flightbooking.data.MockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Airport Navigation ViewModel
 * Manages airport navigation, amenities, and wayfinding
 */
class AirportNavigationViewModel : ViewModel() {

    private val _navigationState = MutableStateFlow(AirportNavigationState())
    val navigationState: StateFlow<AirportNavigationState> = _navigationState.asStateFlow()

    init {
        loadAirportData()
    }

    /**
     * Load airport data (terminals, gates, amenities)
     */
    private fun loadAirportData() {
        viewModelScope.launch {
            _navigationState.value = _navigationState.value.copy(isLoading = true)
            
            try {
                // Simulate API call
                kotlinx.coroutines.delay(500)
                
                val terminal = MockData.getSampleTerminal()
                val amenities = terminal.amenities
                
                _navigationState.value = _navigationState.value.copy(
                    isLoading = false,
                    currentTerminal = terminal,
                    allAmenities = amenities,
                    filteredAmenities = amenities,
                    error = null
                )
            } catch (e: Exception) {
                _navigationState.value = _navigationState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load airport data"
                )
            }
        }
    }

    /**
     * Set user's current location
     */
    fun setCurrentLocation(position: Position) {
        _navigationState.value = _navigationState.value.copy(
            currentLocation = position
        )
        
        // Update distances to all amenities
        updateAmenityDistances()
    }

    /**
     * Set destination (gate or amenity)
     */
    fun setDestination(position: Position, name: String) {
        val currentLocation = _navigationState.value.currentLocation
        
        if (currentLocation != null) {
            val route = calculateRoute(currentLocation, position)
            val distance = calculateDistance(currentLocation, position)
            
            _navigationState.value = _navigationState.value.copy(
                destination = position,
                destinationName = name,
                navigationRoute = route,
                distanceToDestination = DistanceInfo(
                    distance = distance.toFloat(),
                    walkingTime = (distance / 80).toInt(), // Average walking speed 80m/min
                    formattedDistance = if (distance < 100) "${distance.toInt()}m" else "${(distance/10).toInt()*10}m"
                )
            )
        } else {
            _navigationState.value = _navigationState.value.copy(
                error = "Please set your current location first"
            )
        }
    }

    /**
     * Clear navigation route
     */
    fun clearNavigation() {
        _navigationState.value = _navigationState.value.copy(
            destination = null,
            destinationName = "",
            navigationRoute = null,
            distanceToDestination = null
        )
    }

    /**
     * Filter amenities by type
     */
    fun filterAmenitiesByType(type: AmenityType?) {
        val allAmenities = _navigationState.value.allAmenities
        
        _navigationState.value = _navigationState.value.copy(
            selectedAmenityType = type,
            filteredAmenities = if (type != null) {
                allAmenities.filter { it.type == type }
            } else {
                allAmenities
            }
        )
        
        updateAmenityDistances()
    }

    /**
     * Search amenities by name or description
     */
    fun searchAmenities(query: String) {
        val allAmenities = _navigationState.value.allAmenities
        val selectedType = _navigationState.value.selectedAmenityType
        
        val filtered = allAmenities.filter { amenity ->
            val matchesType = selectedType == null || amenity.type == selectedType
            val matchesQuery = query.isEmpty() || 
                amenity.name.contains(query, ignoreCase = true) ||
                amenity.description.contains(query, ignoreCase = true)
            matchesType && matchesQuery
        }
        
        _navigationState.value = _navigationState.value.copy(
            searchQuery = query,
            filteredAmenities = filtered
        )
        
        updateAmenityDistances()
    }

    /**
     * Sort amenities by distance
     */
    fun sortAmenitiesByDistance() {
        val currentLocation = _navigationState.value.currentLocation
        if (currentLocation == null) return
        
        val sorted = _navigationState.value.filteredAmenities.sortedBy { amenity ->
            calculateDistance(currentLocation, amenity.position)
        }
        
        _navigationState.value = _navigationState.value.copy(
            filteredAmenities = sorted
        )
    }

    /**
     * Find nearest amenity of a specific type
     */
    fun findNearestAmenity(type: AmenityType) {
        val currentLocation = _navigationState.value.currentLocation
        if (currentLocation == null) {
            _navigationState.value = _navigationState.value.copy(
                error = "Please set your current location first"
            )
            return
        }
        
        val amenitiesOfType = _navigationState.value.allAmenities.filter { it.type == type }
        
        if (amenitiesOfType.isEmpty()) {
            _navigationState.value = _navigationState.value.copy(
                error = "No ${type.name.lowercase()} found in this terminal"
            )
            return
        }
        
        val nearest = amenitiesOfType.minByOrNull { amenity ->
            calculateDistance(currentLocation, amenity.position)
        }
        
        nearest?.let {
            setDestination(it.position, it.name)
        }
    }

    /**
     * Get gate information
     */
    fun getGateInfo(gateNumber: String): Gate? {
        return _navigationState.value.currentTerminal?.gates?.find {
            it.number == gateNumber
        }
    }

    /**
     * Navigate to gate
     */
    fun navigateToGate(gateNumber: String) {
        val gate = getGateInfo(gateNumber)
        
        if (gate != null) {
            // Set a default current location if not already set
            // (e.g., airport entrance or security checkpoint)
            if (_navigationState.value.currentLocation == null) {
                setCurrentLocation(Position(300f, 250f, 1)) // Default starting position
            }
            
            setDestination(gate.position, "Gate $gateNumber")
        } else {
            _navigationState.value = _navigationState.value.copy(
                error = "Gate $gateNumber not found"
            )
        }
    }

    /**
     * Get accessibility features near location
     */
    fun getAccessibilityFeatures(position: Position, radiusMeters: Double = 50.0): List<Amenity> {
        return _navigationState.value.allAmenities.filter { amenity ->
            amenity.isAccessible &&
            calculateDistance(position, amenity.position) <= radiusMeters
        }
    }

    /**
     * Update distances to all filtered amenities
     */
    private fun updateAmenityDistances() {
        val currentLocation = _navigationState.value.currentLocation ?: return
        
        val amenitiesWithDistance = _navigationState.value.filteredAmenities.map { amenity ->
            val distance = calculateDistance(currentLocation, amenity.position)
            amenity to DistanceInfo(
                distance = distance.toFloat(),
                walkingTime = (distance / 80).toInt(),
                formattedDistance = if (distance < 100) "${distance.toInt()}m" else "${(distance/10).toInt()*10}m"
            )
        }
        
        // Store in a map for easy lookup
        _navigationState.value = _navigationState.value.copy(
            amenityDistances = amenitiesWithDistance.toMap()
        )
    }

    /**
     * Calculate route between two positions
     */
    private fun calculateRoute(from: Position, to: Position): NavigationRoute {
        // Simple route calculation - in real app, this would use pathfinding algorithm
        val distance = calculateDistance(from, to)
        val walkingTime = (distance / 80).toInt() // 80 meters per minute
        
        // Generate simple instructions
        val instructions = mutableListOf<String>()
        
        // Determine direction
        val deltaX = to.x - from.x
        val deltaY = to.y - from.y
        
        if (deltaX.toInt() != 0) {
            val direction = if (deltaX > 0) "right" else "left"
            instructions.add("Turn $direction and walk ${kotlin.math.abs(deltaX).toInt()} meters")
        }
        
        if (deltaY.toInt() != 0) {
            val direction = if (deltaY > 0) "forward" else "backward"
            instructions.add("Continue $direction for ${kotlin.math.abs(deltaY).toInt()} meters")
        }
        
        // Check for floor changes
        if (from.floor != to.floor) {
            val direction = if (to.floor > from.floor) "up" else "down"
            instructions.add("Take stairs or elevator $direction to level ${to.floor}")
        }
        
        instructions.add("You have arrived at your destination")
        
        return NavigationRoute(
            start = from,
            end = to,
            waypoints = listOf(from, to), // Simplified - real app would have intermediate points
            distance = distance.toFloat(),
            estimatedTime = walkingTime,
            instructions = instructions.mapIndexed { index, text ->
                NavigationInstruction(
                    step = index + 1,
                    instruction = text,
                    position = if (index == 0) from else to,
                    distance = if (index == 0) distance.toFloat() else 0f,
                    icon = if (index == instructions.size - 1) NavigationIcon.DESTINATION else NavigationIcon.STRAIGHT
                )
            }
        )
    }

    /**
     * Calculate distance between two positions (in meters)
     */
    private fun calculateDistance(from: Position, to: Position): Double {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = (to.floor - from.floor) * 4.0 // Assume 4 meters per floor
        
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Get amenities by category
     */
    fun getAmenitiesByCategory(): Map<AmenityType, List<Amenity>> {
        return _navigationState.value.allAmenities.groupBy { it.type }
    }

    /**
     * Get popular amenities (based on rating or usage)
     */
    fun getPopularAmenities(limit: Int = 5): List<Amenity> {
        return _navigationState.value.allAmenities
            .filter { it.isOpen }
            .sortedByDescending { it.name.length } // Placeholder - real app would use ratings
            .take(limit)
    }

    /**
     * Get nearby gates
     */
    fun getNearbyGates(position: Position, radiusMeters: Double = 100.0): List<Gate> {
        return _navigationState.value.currentTerminal?.gates?.filter { gate ->
            calculateDistance(position, gate.position) <= radiusMeters
        } ?: emptyList()
    }

    /**
     * Check if amenity is open
     */
    fun isAmenityOpen(amenity: Amenity): Boolean {
        // In real app, this would check current time against operating hours
        return amenity.isOpen
    }

    /**
     * Get estimated wait time for security
     */
    fun getSecurityWaitTime(): String {
        // In real app, this would fetch real-time data
        return "15-20 minutes"
    }

    /**
     * Get flight boarding status for gate
     */
    fun getGateBoardingStatus(gateNumber: String): String {
        val gate = getGateInfo(gateNumber)
        return gate?.status?.name ?: "Unknown"
    }

    /**
     * Toggle map view (2D/3D)
     */
    fun toggleMapView() {
        // Map view toggle - can be implemented when 3D view is added
        // For now, this is a placeholder
    }

    /**
     * Set map zoom level
     */
    fun setMapZoom(zoom: Float) {
        _navigationState.value = _navigationState.value.copy(
            mapZoom = zoom.coerceIn(0.5f, 3.0f)
        )
    }

    /**
     * Report an issue with amenity
     */
    fun reportIssue(amenity: Amenity, issue: String) {
        viewModelScope.launch {
            // In real app, this would send to backend
            // Success notification would be shown via UI
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        // Error handling can be implemented when error state is added
    }

    /**
     * Refresh airport data
     */
    fun refresh() {
        loadAirportData()
    }
}

/**
 * Extension function to get distance info for an amenity
 */
fun AirportNavigationState.getDistanceInfo(amenity: Amenity): DistanceInfo? {
    return userLocation?.position?.let { userPos ->
        DistanceInfo.calculate(userPos, amenity.position)
    }
}

// Made with Bob
