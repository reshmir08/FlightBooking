package com.example.flightbooking.data.models

/**
 * Airport navigation and terminal map data models
 */

/**
 * Airport terminal information
 */
data class Terminal(
    val id: String,
    val name: String,
    val airport: Airport,
    val gates: List<Gate>,
    val amenities: List<Amenity>,
    val mapData: TerminalMapData? = null
)

/**
 * Gate information
 */
data class Gate(
    val id: String,
    val number: String,
    val terminal: String,
    val position: Position,
    val status: GateStatus = GateStatus.AVAILABLE,
    val currentFlight: String? = null,
    val boardingTime: String? = null
)

/**
 * Gate status
 */
enum class GateStatus {
    AVAILABLE,
    BOARDING,
    DEPARTED,
    DELAYED,
    CANCELLED,
    MAINTENANCE
}

/**
 * Position coordinates within terminal
 */
data class Position(
    val x: Float,
    val y: Float,
    val floor: Int = 1
) {
    fun distanceTo(other: Position): Float {
        if (floor != other.floor) return Float.MAX_VALUE
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * Amenity types
 */
enum class AmenityType {
    RESTROOM,
    FOOD_COURT,
    RESTAURANT,
    CAFE,
    COFFEE_SHOP,
    LOUNGE,
    CHARGING_STATION,
    ATM,
    CURRENCY_EXCHANGE,
    DUTY_FREE,
    PHARMACY,
    MEDICAL_CENTER,
    INFORMATION_DESK,
    BAGGAGE_CLAIM,
    CHECK_IN,
    SECURITY,
    CUSTOMS,
    PRAYER_ROOM,
    NURSING_ROOM,
    PLAY_AREA,
    WIFI_ZONE,
    SMOKING_AREA,
    ELEVATOR,
    ESCALATOR,
    STAIRS,
    MOVING_WALKWAY
}

/**
 * Amenity information
 */
data class Amenity(
    val id: String,
    val name: String,
    val type: AmenityType,
    val position: Position,
    val terminal: String,
    val description: String = "",
    val isAccessible: Boolean = true,
    val isOpen: Boolean = true,
    val openingHours: String? = null,
    val rating: Float = 0f,
    val imageUrl: String? = null,
    val additionalInfo: Map<String, String> = emptyMap()
)

/**
 * Terminal map SVG data
 */
data class TerminalMapData(
    val svgPath: String,
    val width: Float,
    val height: Float,
    val scale: Float = 1.0f,
    val layers: List<MapLayer> = emptyList()
)

/**
 * Map layer for different floor levels
 */
data class MapLayer(
    val floor: Int,
    val name: String,
    val svgData: String,
    val isVisible: Boolean = true
)

/**
 * Navigation route
 */
data class NavigationRoute(
    val start: Position,
    val end: Position,
    val waypoints: List<Position>,
    val distance: Float,
    val estimatedTime: Int, // minutes
    val instructions: List<NavigationInstruction>
)

/**
 * Navigation instruction
 */
data class NavigationInstruction(
    val step: Int,
    val instruction: String,
    val position: Position,
    val distance: Float,
    val icon: NavigationIcon
)

/**
 * Types of geofence zones in the airport
 */
enum class GeofenceZoneType {
    SECURITY,
    GATE,
    LOUNGE,
    RESTROOM,
    FOOD_COURT,
    BOARDING,
    ARRIVAL,
    CUSTOM
}

/**
 * Virtual geofence zone around an important airport location.
 *
 * When the user's position enters the circle defined by [center] + [radiusMeters],
 * [announcementMessage] is spoken once via TextToSpeech.
 */
data class GeofenceZone(
    val id: String,
    val name: String,
    val center: Position,
    val radiusMeters: Float,
    val announcementMessage: String,
    val type: GeofenceZoneType
)

/**
 * Navigation icons
 */
enum class NavigationIcon {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    STAIRS_UP,
    STAIRS_DOWN,
    ELEVATOR,
    ESCALATOR_UP,
    ESCALATOR_DOWN,
    DESTINATION,
    TURN_AROUND
}

/**
 * User location within terminal
 */
data class UserLocation(
    val position: Position,
    val terminal: String,
    val nearestGate: Gate? = null,
    val nearestAmenities: List<Amenity> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Search filters for amenities
 */
data class AmenityFilters(
    val types: Set<AmenityType> = emptySet(),
    val accessibleOnly: Boolean = false,
    val openNow: Boolean = false,
    val maxDistance: Float? = null,
    val minRating: Float? = null
)

/**
 * Airport navigation state
 */
data class AirportNavigationState(
    val currentTerminal: Terminal? = null,
    val userLocation: UserLocation? = null,
    val selectedGate: Gate? = null,
    val selectedAmenity: Amenity? = null,
    val navigationRoute: NavigationRoute? = null,
    val amenities: List<Amenity> = emptyList(),
    val filters: AmenityFilters = AmenityFilters(),
    val searchQuery: String = "",
    val isNavigating: Boolean = false,
    val showAccessibilityFeatures: Boolean = false,
    val mapZoom: Float = 1.0f,
    val mapOffset: Pair<Float, Float> = Pair(0f, 0f),
    // Additional properties for UI state management
    val isLoading: Boolean = false,
    val error: String? = null,
    val allAmenities: List<Amenity> = emptyList(),
    val filteredAmenities: List<Amenity> = emptyList(),
    val selectedAmenityType: AmenityType? = null,
    val currentLocation: Position? = null,
    val destination: Position? = null,
    val destinationName: String = "",
    val distanceToDestination: DistanceInfo? = null,
    val amenityDistances: Map<Amenity, DistanceInfo> = emptyMap(),
    val is3DView: Boolean = false,
    // Voice navigation state
    val currentStepIndex: Int = 0,
    val isMuted: Boolean = false,
    val isDemoRunning: Boolean = false,
    // Indoor map state
    val mapTransform: MapTransform = MapTransform(),
    val visibleFloor: Int = 1,
    val userHeading: Float = 0f           // degrees, 0 = east, clockwise
)

/**
 * Distance calculation result
 */
data class DistanceInfo(
    val distance: Float,           // meters
    val walkingTime: Int,          // minutes (kept for backward compat)
    val formattedDistance: String, // e.g. "47m", "1.2km"
    val formattedTime: String = "" // e.g. "< 1 min", "3 min"
) {
    val meters: Float get() = distance
    val walkingTimeMinutes: Int get() = walkingTime

    companion object {
        fun calculate(from: Position, to: Position): DistanceInfo {
            val distM  = from.distanceTo(to)
            val secs   = (distM / 1.4f).toInt()
            val mins   = secs / 60
            val fmtDist = when {
                distM < 1000 -> "${distM.toInt()}m"
                else         -> "${"%.1f".format(distM / 1000)}km"
            }
            val fmtTime = when {
                secs < 60  -> "< 1 min"
                mins == 1  -> "1 min"
                else       -> "$mins min"
            }
            return DistanceInfo(distM, mins, fmtDist, fmtTime)
        }
    }
}

// Made with Bob
