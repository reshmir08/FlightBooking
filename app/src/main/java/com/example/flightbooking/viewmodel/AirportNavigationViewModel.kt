package com.example.flightbooking.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightbooking.data.models.*
import com.example.flightbooking.data.MockData
import com.example.flightbooking.data.MockFloorPlanData
import com.example.flightbooking.navigation.MapCoordinates
import com.example.flightbooking.navigation.VoiceNavigationEngine
import com.example.flightbooking.navigation.VoiceSpeaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Airport Navigation ViewModel
 * Manages airport navigation, amenities, wayfinding and voice guidance.
 */
class AirportNavigationViewModel : ViewModel() {

    private val _navigationState = MutableStateFlow(AirportNavigationState())
    val navigationState: StateFlow<AirportNavigationState> = _navigationState.asStateFlow()

    // ── Voice navigation ──────────────────────────────────────────────────────

    /** Android TTS wrapper — initialised lazily from the composable via [initVoiceSpeaker]. */
    private var voiceSpeaker: VoiceSpeaker? = null

    /** Pure-Kotlin engine that decides what to announce. Created per route. */
    private var voiceEngine: VoiceNavigationEngine? = null

    /** All virtual geofence zones loaded at startup. */
    private var geofenceZones: List<GeofenceZone> = emptyList()

    /**
     * Holds the start announcement text when [setDestination] is called before
     * [initVoiceSpeaker] has run (e.g. navigated from GateFinderScreen before
     * NavigationScreen is composed). Replayed in [initVoiceSpeaker].
     */
    private var pendingStartAnnouncement: String = ""

    // ── Demo mode ─────────────────────────────────────────────────────────────

    private var demoJob: Job? = null

    // ── Indoor map ────────────────────────────────────────────────────────────

    /** Cached floor plans keyed by floor number. */
    private val floorPlanCache = mutableMapOf<Int, FloorPlan>()

    /** Last known position, used to compute heading between updates. */
    private var lastPosition: Position? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadAirportData()
    }

    // ── Voice speaker lifecycle ───────────────────────────────────────────────

    /**
     * Initialise the [VoiceSpeaker]. Must be called from the composable's
     * `DisposableEffect` so the Android Context is available.
     */
    fun initVoiceSpeaker(context: Context) {
        if (voiceSpeaker == null) {
            voiceSpeaker = VoiceSpeaker(context)
        }
        // Sync mute state in case it changed before the speaker was created
        voiceSpeaker?.isMuted = _navigationState.value.isMuted
        // Replay start announcement if it was queued before TTS existed
        if (pendingStartAnnouncement.isNotBlank()) {
            val msg = pendingStartAnnouncement
            pendingStartAnnouncement = ""
            voiceSpeaker?.speak(msg, flushQueue = true)
        }
    }

    /**
     * Release TTS resources. Must be called when the composable is disposed.
     */
    fun shutdownVoiceSpeaker() {
        voiceSpeaker?.shutdown()
        voiceSpeaker = null
    }

    /**
     * Toggle mute state for voice announcements.
     */
    fun toggleMute() {
        val muted = !_navigationState.value.isMuted
        _navigationState.value = _navigationState.value.copy(isMuted = muted)
        voiceSpeaker?.isMuted = muted
        if (!muted) {
            // Un-muting: re-announce current step so the user knows where they are
            val step = _navigationState.value.navigationRoute
                ?.instructions
                ?.getOrNull(_navigationState.value.currentStepIndex)
            step?.let { voiceSpeaker?.speak(it.instruction, flushQueue = true) }
        } else {
            voiceSpeaker?.stopSpeaking()
        }
    }

    /**
     * Manually jump to a specific step (e.g. when user taps Previous/Next).
     */
    fun jumpToStep(index: Int) {
        val instructions = _navigationState.value.navigationRoute?.instructions ?: return
        val clamped = index.coerceIn(0, instructions.lastIndex)
        _navigationState.value = _navigationState.value.copy(currentStepIndex = clamped)
        voiceSpeaker?.speak(instructions[clamped].instruction, flushQueue = true)
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadAirportData() {
        viewModelScope.launch {
            _navigationState.value = _navigationState.value.copy(isLoading = true)

            try {
                kotlinx.coroutines.delay(500)

                val terminal = MockData.getSampleTerminal()
                val amenities = terminal.amenities
                geofenceZones = MockData.getSampleGeofenceZones()

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

    // ── Location & navigation ─────────────────────────────────────────────────

    /**
     * Update the user's current location.
     *
     * This is the single entry point called every time the user moves —
     * whether from a real sensor, a manual tap, or the demo simulation.
     * The voice engine evaluates the new position and any resulting messages
     * are spoken immediately.
     */
    fun setCurrentLocation(position: Position) {
        // Compute heading from previous position (atan2 gives radians; convert to degrees)
        val prev = lastPosition
        val heading = if (prev != null && prev.floor == position.floor) {
            val dx = position.x - prev.x
            val dy = position.y - prev.y
            if (dx != 0f || dy != 0f) {
                // atan2(dy, dx): heading 0°=east, 90°=south (canvas y increases downward).
                // drawUserMarker uses rotate(heading + 90°) on a north-pointing triangle,
                // so east→+90° rotates tip to point right, south→+180° points down, etc.
                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            } else {
                _navigationState.value.userHeading
            }
        } else {
            _navigationState.value.userHeading
        }
        lastPosition = position

        // Auto-switch visible floor when navigation crosses a floor boundary
        val newFloor = position.floor
        val floorChanged = newFloor != _navigationState.value.visibleFloor && voiceEngine != null

        _navigationState.value = _navigationState.value.copy(
            currentLocation = position,
            userHeading = heading,
            visibleFloor = if (floorChanged) newFloor else _navigationState.value.visibleFloor
        )
        updateAmenityDistances()

        // Update distance on every location tick — exact meters, no rounding,
        // so the counter visibly decrements during demo walk.
        val destination = _navigationState.value.destination
        if (destination != null) {
            val distM  = calculateDistance(position, destination)
            val secs   = (distM / 1.4).toInt()
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
            _navigationState.value = _navigationState.value.copy(
                distanceToDestination = DistanceInfo(
                    distance          = distM.toFloat(),
                    walkingTime       = mins,
                    formattedDistance = fmtDist,
                    formattedTime     = fmtTime
                )
            )
        }

        // Voice engine evaluation
        val engine = voiceEngine ?: return
        val messages = engine.onLocationUpdate(position)
        messages.forEach { msg -> voiceSpeaker?.speak(msg) }

        // Sync step index driven by the engine into UI state.
        // Do NOT advance the UI to the DESTINATION step until the user is physically
        // there (engine.currentStepIndex goes past the last instruction index).
        // If we mirror the DESTINATION step index while the user is still mid-route,
        // the top card immediately shows "You have arrived" even though they aren't.
        val route = _navigationState.value.navigationRoute
        val uiStepIndex = if (
            route != null &&
            engine.currentStepIndex < route.instructions.size &&
            route.instructions[engine.currentStepIndex].icon == NavigationIcon.DESTINATION
        ) {
            // Engine has advanced to the DESTINATION step but user hasn't reached it yet.
            // Keep the UI on the previous step (the last real navigation instruction).
            engine.currentStepIndex - 1
        } else {
            engine.currentStepIndex
        }
        if (uiStepIndex != _navigationState.value.currentStepIndex) {
            _navigationState.value = _navigationState.value.copy(
                currentStepIndex = uiStepIndex
            )
        }
    }

    /**
     * Set a destination and build a navigation route.
     * A new [VoiceNavigationEngine] is created so all previous state is cleared.
     */
    fun setDestination(position: Position, name: String) {
        val currentLocation = _navigationState.value.currentLocation

        if (currentLocation != null) {
            val route = calculateRoute(currentLocation, position)

            // Build the voice engine for this route, passing the destination name so
            // announcements read "Gate A6 is 20 meters ahead" instead of generic text.
            // Start at step index 1: step 0 is the orientation instruction announced
            // immediately below in startMsg, so the engine begins watching for the elbow.
            voiceEngine = VoiceNavigationEngine(route, geofenceZones, destinationName = name,
                                                initialStepIndex = 1)
            voiceSpeaker?.resetLastMessage()

            _navigationState.value = _navigationState.value.copy(
                destination = position,
                destinationName = name,
                navigationRoute = route,
                currentStepIndex = 1,   // step 0 already announced in startMsg below
                distanceToDestination = DistanceInfo.calculate(
                    currentLocation, position
                )
            )

            // Build the start announcement: destination, total distance, first instruction.
            val firstStep   = route.instructions.firstOrNull()
            val distM       = DistanceInfo.calculate(currentLocation, position)
            val distClause  = "${distM.formattedDistance} away. "
            val firstClause = firstStep?.instruction ?: ""
            val startMsg    = "Navigation started. $name is $distClause$firstClause".trim()

            if (voiceSpeaker != null) {
                // TTS already exists (user started nav while NavigationScreen was open)
                voiceSpeaker?.speak(startMsg, flushQueue = true)
            } else {
                // TTS does not exist yet — NavigationScreen hasn't been composed.
                // Store the message; initVoiceSpeaker() will replay it once TTS is ready.
                pendingStartAnnouncement = startMsg
            }
        } else {
            _navigationState.value = _navigationState.value.copy(
                error = "Please set your current location first"
            )
        }
    }

    /**
     * Stop navigation and clear all route state.
     */
    fun clearNavigation() {
        stopDemo()
        voiceEngine?.reset()
        voiceEngine = null
        voiceSpeaker?.stopSpeaking()

        _navigationState.value = _navigationState.value.copy(
            destination = null,
            destinationName = "",
            navigationRoute = null,
            distanceToDestination = null,
            currentStepIndex = 0
        )
    }

    // ── Demo / simulation mode ─────────────────────────────────────────────────

    /**
     * Start a simulated walk along the active route.
     *
     * The user's position is interpolated linearly between consecutive waypoints.
     * Every [DEMO_TICK_MS] milliseconds the position advances by [DEMO_STEP_METERS]
     * meters, calling [setCurrentLocation] so voice + UI react naturally.
     */
    fun startDemo() {
        val route = _navigationState.value.navigationRoute ?: return
        if (_navigationState.value.isDemoRunning) return

        _navigationState.value = _navigationState.value.copy(isDemoRunning = true)

        demoJob = viewModelScope.launch {
            val waypoints = route.waypoints

            for (i in 0 until waypoints.lastIndex) {
                val from = waypoints[i]
                val to = waypoints[i + 1]
                val segmentDistance = from.distanceTo(to)
                if (segmentDistance == Float.MAX_VALUE || segmentDistance == 0f) continue

                val steps = (segmentDistance / DEMO_STEP_METERS).toInt().coerceAtLeast(1)

                for (step in 0..steps) {
                    if (!_navigationState.value.isDemoRunning) return@launch

                    val t = step.toFloat() / steps
                    val interpolated = Position(
                        x = from.x + (to.x - from.x) * t,
                        y = from.y + (to.y - from.y) * t,
                        floor = if (t < 0.5f) from.floor else to.floor
                    )
                    setCurrentLocation(interpolated)
                    delay(DEMO_TICK_MS)
                }
            }

            // Ensure we land exactly on the final waypoint
            waypoints.lastOrNull()?.let { setCurrentLocation(it) }
            _navigationState.value = _navigationState.value.copy(isDemoRunning = false)
        }
    }

    /**
     * Stop the demo simulation coroutine.
     */
    fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        _navigationState.value = _navigationState.value.copy(isDemoRunning = false)
    }

    // ── Amenity helpers ───────────────────────────────────────────────────────

    fun filterAmenitiesByType(type: AmenityType?) {
        val allAmenities = _navigationState.value.allAmenities

        _navigationState.value = _navigationState.value.copy(
            selectedAmenityType = type,
            filteredAmenities = if (type != null) allAmenities.filter { it.type == type }
            else allAmenities
        )
        updateAmenityDistances()
    }

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

    fun sortAmenitiesByDistance() {
        val currentLocation = _navigationState.value.currentLocation ?: return
        val sorted = _navigationState.value.filteredAmenities.sortedBy { amenity ->
            calculateDistance(currentLocation, amenity.position)
        }
        _navigationState.value = _navigationState.value.copy(filteredAmenities = sorted)
    }

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

        val nearest = amenitiesOfType.minByOrNull { calculateDistance(currentLocation, it.position) }
        nearest?.let { setDestination(it.position, it.name) }
    }

    fun getGateInfo(gateNumber: String): Gate? =
        _navigationState.value.currentTerminal?.gates?.find { it.number == gateNumber }

    fun navigateToGate(gateNumber: String) {
        val gate = getGateInfo(gateNumber)
        if (gate != null) {
            if (_navigationState.value.currentLocation == null) {
                // Bug 4 fix: default start is the terminal entrance (300, 450) — well
                // away from the security geofence at (300, 250). The old value caused
                // the security announcement to fire immediately on every navigation start.
                setCurrentLocation(Position(300f, 450f, 1))
            }
            setDestination(gate.position, "Gate $gateNumber")
        } else {
            _navigationState.value = _navigationState.value.copy(error = "Gate $gateNumber not found")
        }
    }

    fun getAccessibilityFeatures(position: Position, radiusMeters: Double = 50.0): List<Amenity> =
        _navigationState.value.allAmenities.filter { amenity ->
            amenity.isAccessible && calculateDistance(position, amenity.position) <= radiusMeters
        }

    private fun updateAmenityDistances() {
        val currentLocation = _navigationState.value.currentLocation ?: return

        val amenitiesWithDistance = _navigationState.value.filteredAmenities.map { amenity ->
            val distance = calculateDistance(currentLocation, amenity.position)
            amenity to DistanceInfo(
                distance = distance.toFloat(),
                walkingTime = (distance / 80).toInt(),
                formattedDistance = if (distance < 100) "${distance.toInt()}m"
                else "${(distance / 10).toInt() * 10}m"
            )
        }
        _navigationState.value = _navigationState.value.copy(
            amenityDistances = amenitiesWithDistance.toMap()
        )
    }

    // ── Route calculation ─────────────────────────────────────────────────────
    //
    // Coordinate system (matches MapCoordinates):
    //   x increases → east  (right on screen)
    //   y increases → south (down on screen)
    //
    // L-shaped route: walk the horizontal leg first (east/west), then the vertical
    // leg (north/south).  The "elbow" is the corner between the two legs.
    //
    // Turn direction at the elbow (standing facing east or west, turning north or south):
    //   deltaX > 0 (going east) + deltaY < 0 (going north) → face east, turn north = LEFT
    //   deltaX > 0 (going east) + deltaY > 0 (going south) → face east, turn south = RIGHT
    //   deltaX < 0 (going west) + deltaY < 0 (going north) → face west, turn north = RIGHT
    //   deltaX < 0 (going west) + deltaY > 0 (going south) → face west, turn south = LEFT
    //   Formula: turn = LEFT when sign(deltaX) != sign(deltaY), RIGHT otherwise.
    //
    // Instruction trigger positions:
    //   Step 1 → fires at `from`  : "Head east/west, walk X meters" (before horizontal leg)
    //   Step 2 → fires at `elbow` : "Turn left/right, walk Y meters" (at the corner)
    //   Step 3 → fires at 3/4 of vertical leg (mid-leg straight reminder)
    //   Last   → fires at `to`   : "You have arrived"

    private fun calculateRoute(from: Position, to: Position): NavigationRoute {
        val distance = calculateDistance(from, to)
        val walkingTime = (distance / 80).toInt()

        val deltaX = to.x - from.x
        val deltaY = to.y - from.y
        val hasHoriz = deltaX.toInt() != 0
        val hasVert  = deltaY.toInt() != 0

        // Elbow = corner of the L (same x as destination, same y as start)
        val elbow = Position(to.x, from.y, from.floor)

        val waypoints: List<Position> = when {
            from.floor != to.floor          -> listOf(from, elbow, to)
            hasHoriz && hasVert             -> listOf(from, elbow, to)
            hasHoriz                        -> listOf(from, to)
            else                            -> listOf(from, to)
        }

        val instructions = mutableListOf<NavigationInstruction>()
        var stepIndex = 1

        // ── Horizontal leg ────────────────────────────────────────────────────
        if (hasHoriz) {
            val horizDir  = if (deltaX > 0) "east" else "west"
            val horizDist = kotlin.math.abs(deltaX).toInt()

            if (hasVert) {
                // L-shaped route: announce the horizontal leg from the start position.
                // Icon is STRAIGHT because user walks straight for this leg.
                instructions.add(
                    NavigationInstruction(
                        step        = stepIndex++,
                        instruction = "Head $horizDir and walk $horizDist meters",
                        position    = from,
                        distance    = kotlin.math.abs(deltaX),
                        icon        = NavigationIcon.STRAIGHT
                    )
                )

                // At the elbow, announce the turn into the vertical leg.
                // Turn = LEFT when sign(deltaX) != sign(deltaY), RIGHT otherwise.
                val turnLeft = (deltaX > 0) != (deltaY > 0)
                val turnWord = if (turnLeft) "left" else "right"
                val turnIcon = if (turnLeft) NavigationIcon.TURN_LEFT else NavigationIcon.TURN_RIGHT
                val vertDist = kotlin.math.abs(deltaY).toInt()
                instructions.add(
                    NavigationInstruction(
                        step        = stepIndex++,
                        instruction = "Turn $turnWord and walk $vertDist meters",
                        position    = elbow,
                        distance    = kotlin.math.abs(deltaY),
                        icon        = turnIcon
                    )
                )
            } else {
                // Purely horizontal — just walk straight
                instructions.add(
                    NavigationInstruction(
                        step        = stepIndex++,
                        instruction = "Walk $horizDir for $horizDist meters",
                        position    = from,
                        distance    = kotlin.math.abs(deltaX),
                        icon        = NavigationIcon.STRAIGHT
                    )
                )
            }
        }

        // ── Vertical leg (only when no horizontal leg, i.e. purely vertical route) ──
        // When there IS a horizontal leg the vertical leg announcement is emitted above
        // at the elbow as the "Turn left/right" step. Here we only handle pure-vertical.
        if (hasVert && !hasHoriz) {
            val vertDir  = if (deltaY < 0) "north" else "south"
            val vertDist = kotlin.math.abs(deltaY).toInt()
            instructions.add(
                NavigationInstruction(
                    step        = stepIndex++,
                    instruction = "Walk $vertDir for $vertDist meters",
                    position    = from,
                    distance    = kotlin.math.abs(deltaY),
                    icon        = NavigationIcon.STRAIGHT
                )
            )
        }

        // ── Floor change ──────────────────────────────────────────────────────
        if (from.floor != to.floor) {
            val dir = if (to.floor > from.floor) "up" else "down"
            instructions.add(
                NavigationInstruction(
                    step        = stepIndex++,
                    instruction = "Take stairs or elevator $dir to level ${to.floor}",
                    position    = to,
                    distance    = kotlin.math.abs((to.floor - from.floor) * 4f),
                    icon        = if (to.floor > from.floor) NavigationIcon.STAIRS_UP
                                  else NavigationIcon.STAIRS_DOWN
                )
            )
        }

        // ── Arrival ───────────────────────────────────────────────────────────
        instructions.add(
            NavigationInstruction(
                step        = stepIndex,
                instruction = "You have arrived at your destination",
                position    = to,
                distance    = 0f,
                icon        = NavigationIcon.DESTINATION
            )
        )

        return NavigationRoute(
            start         = from,
            end           = to,
            waypoints     = waypoints,
            distance      = distance.toFloat(),
            estimatedTime = walkingTime,
            instructions  = instructions
        )
    }

    private fun calculateDistance(from: Position, to: Position): Double {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = (to.floor - from.floor) * 4.0
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    // ── Indoor map helpers ────────────────────────────────────────────────────

    /** Return the [FloorPlan] for [floor], loading from [MockFloorPlanData] if not cached. */
    fun loadFloorPlan(floor: Int): FloorPlan =
        floorPlanCache.getOrPut(floor) { MockFloorPlanData.getFloorPlan(floor) }

    /** Update the map pan/zoom transform (called from the composable on gesture events). */
    fun updateMapTransform(transform: MapTransform) {
        _navigationState.value = _navigationState.value.copy(mapTransform = transform)
    }

    /** Reset map to default zoom and pan. */
    fun resetMapView() {
        _navigationState.value = _navigationState.value.copy(mapTransform = MapTransform())
    }

    /** Manually switch the visible floor (e.g. user taps floor selector). */
    fun setVisibleFloor(floor: Int) {
        loadFloorPlan(floor) // pre-warm cache
        _navigationState.value = _navigationState.value.copy(visibleFloor = floor)
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    fun getAmenitiesByCategory(): Map<AmenityType, List<Amenity>> =
        _navigationState.value.allAmenities.groupBy { it.type }

    fun getPopularAmenities(limit: Int = 5): List<Amenity> =
        _navigationState.value.allAmenities
            .filter { it.isOpen }
            .sortedByDescending { it.name.length }
            .take(limit)

    fun getNearbyGates(position: Position, radiusMeters: Double = 100.0): List<Gate> =
        _navigationState.value.currentTerminal?.gates?.filter { gate ->
            calculateDistance(position, gate.position) <= radiusMeters
        } ?: emptyList()

    fun isAmenityOpen(amenity: Amenity): Boolean = amenity.isOpen

    fun getSecurityWaitTime(): String = "15-20 minutes"

    fun getGateBoardingStatus(gateNumber: String): String =
        getGateInfo(gateNumber)?.status?.name ?: "Unknown"

    /**
     * Recalculate the current route from the latest known position.
     * This rebuilds the engine (clearing wrong-route state) and re-announces the new first step.
     */
    fun recalculate() {
        val current = _navigationState.value.currentLocation ?: return
        val dest    = _navigationState.value.destination ?: return
        val name    = _navigationState.value.destinationName
        setDestination(dest, name)
        voiceSpeaker?.speak("Recalculating route.", flushQueue = true)
    }

    fun toggleMapView() { /* placeholder for future 3D view */ }

    fun setMapZoom(zoom: Float) {
        _navigationState.value = _navigationState.value.copy(
            mapZoom = zoom.coerceIn(0.5f, 3.0f)
        )
    }

    fun reportIssue(amenity: Amenity, issue: String) {
        viewModelScope.launch { /* In real app: send to backend */ }
    }

    fun clearError() {
        _navigationState.value = _navigationState.value.copy(error = null)
    }

    fun refresh() {
        loadAirportData()
    }

    override fun onCleared() {
        super.onCleared()
        stopDemo()
        shutdownVoiceSpeaker()
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Milliseconds between each simulated position tick in demo mode. */
        const val DEMO_TICK_MS = 1000L

        /** Meters advanced per simulation tick. */
        const val DEMO_STEP_METERS = 10f
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
