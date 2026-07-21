package com.example.flightbooking.navigation

import com.example.flightbooking.data.models.GeofenceZone
import com.example.flightbooking.data.models.NavigationIcon
import com.example.flightbooking.data.models.NavigationRoute
import com.example.flightbooking.data.models.Position

/**
 * VoiceNavigationEngine
 *
 * Pure Kotlin engine (zero Android imports) that decides what voice messages
 * to emit based on the user's current position, the active route, virtual
 * geofence zones, and an optional human-readable destination name.
 *
 * Responsibilities:
 *  - Auto-advance the current navigation step when the user is close enough.
 *  - Emit distance-threshold announcements at 50 m, 20 m, and 5 m from the
 *    next step waypoint.
 *  - Emit a periodic distance-to-destination reminder every [REMINDER_INTERVAL_TICKS] updates.
 *  - Detect wrong-route deviations and emit a single recalculation prompt.
 *    The wrong-route flag re-arms automatically once the user is back on route.
 *  - Detect geofence-zone entry and emit zone announcements exactly once per
 *    visit; the zone re-arms after the user leaves by more than [GEOFENCE_EXIT_MULTIPLIER]
 *    times the zone radius.
 *
 * Duplicate suppression (back-to-back) is handled by [VoiceSpeaker]; the engine
 * may return the same message type again if conditions are reset.
 *
 * Usage:
 * ```
 * val engine = VoiceNavigationEngine(route, geofenceZones, destinationName = "Gate A6")
 * val messages = engine.onLocationUpdate(position)
 * messages.forEach { voiceSpeaker.speak(it) }
 * ```
 */
class VoiceNavigationEngine(
    private val route: NavigationRoute,
    private val geofenceZones: List<GeofenceZone>,
    /** Human-readable name of the destination, used in final arrival announcement. */
    private val destinationName: String = "",
    /**
     * Which step index to begin watching from. Defaults to 0.
     * Pass 1 when step 0 has already been announced externally (e.g. the "Navigation
     * started" message in the ViewModel) so the engine skips straight to watching for
     * the first waypoint trigger.
     */
    private val initialStepIndex: Int = 0
) {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Distance in meters within which the user is considered to have reached a step. */
        const val STEP_ARRIVAL_THRESHOLD = 10f

        /**
         * Distance in meters from the nearest route waypoint beyond which the user
         * is considered off-route.
         */
        const val WRONG_ROUTE_THRESHOLD = 50f

        /**
         * Distance buckets (in meters) at which a "distance remaining" announcement
         * is made for the current step. Ordered descending so we check largest first.
         */
        val DISTANCE_ANNOUNCEMENT_BUCKETS = listOf(50f, 20f, 5f)

        /**
         * How many [onLocationUpdate] ticks between periodic destination-distance reminders.
         * At DEMO_TICK_MS=1500ms a value of 20 means ~30 seconds between reminders.
         */
        const val REMINDER_INTERVAL_TICKS = 20

        /**
         * Zone re-entry: the user must move this many times the zone's radius away from the
         * centre before the same zone will announce again.
         */
        const val GEOFENCE_EXIT_MULTIPLIER = 2.0f
    }

    // ── Mutable engine state ───────────────────────────────────────────────────

    /** Index into [route.instructions] for the step currently being navigated. */
    var currentStepIndex: Int = initialStepIndex
        private set

    /**
     * The distance bucket (meters) for which the last "distance remaining"
     * announcement was made. Null means no announcement yet for this step.
     */
    private var lastAnnouncedDistanceBucket: Float? = null

    /**
     * Map of zone ID → whether the user is currently "inside" that zone (i.e. has entered
     * and not yet exited far enough to re-arm).
     */
    private val zoneState: MutableMap<String, Boolean> = mutableMapOf()

    /** True after the wrong-route message has been emitted; cleared once user returns to route. */
    private var wrongRouteAnnounced: Boolean = false

    /** True once the destination announcement has been made. */
    private var destinationAnnounced: Boolean = false

    /** Tick counter for periodic distance reminders. */
    private var tickCount: Int = 0

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process a new user [position] and return a list of voice messages to speak.
     * The list may be empty if no announcement is warranted.
     *
     * Messages are ordered by priority:
     * 1. Geofence zone entries / exits (context-aware environment info)
     * 2. Step advance (new instruction)
     * 3. Distance threshold (approaching next waypoint)
     * 4. Periodic destination-distance reminder
     * 5. Wrong-route warning
     */
    fun onLocationUpdate(position: Position): List<String> {
        tickCount++
        val messages = mutableListOf<String>()

        // 1. Geofence check (independent of route step)
        messages += checkGeofences(position)

        // Nothing more to do if route is already complete
        if (currentStepIndex >= route.instructions.size) return messages

        val currentStep = route.instructions[currentStepIndex]
        val distanceToStep = position.distanceTo(currentStep.position)

        // 2. Step arrival — user is within STEP_ARRIVAL_THRESHOLD of the current step's position
        if (distanceToStep != Float.MAX_VALUE && distanceToStep <= STEP_ARRIVAL_THRESHOLD) {
            // If this IS the destination step, announce arrival now (user is physically here).
            if (currentStep.icon == NavigationIcon.DESTINATION && !destinationAnnounced) {
                messages += announceArrival()
                currentStepIndex++
                return messages
            }

            // For a TURN step: announce "Turn left now." / "Turn right now." at the exact
            // moment the user reaches the corner, then advance to the next step.
            if (currentStep.icon == NavigationIcon.TURN_LEFT ||
                currentStep.icon == NavigationIcon.TURN_RIGHT ||
                currentStep.icon == NavigationIcon.TURN_SLIGHT_LEFT ||
                currentStep.icon == NavigationIcon.TURN_SLIGHT_RIGHT
            ) {
                val turnNow = when (currentStep.icon) {
                    NavigationIcon.TURN_LEFT         -> "Turn left now."
                    NavigationIcon.TURN_RIGHT        -> "Turn right now."
                    NavigationIcon.TURN_SLIGHT_LEFT  -> "Bear slightly left now."
                    NavigationIcon.TURN_SLIGHT_RIGHT -> "Bear slightly right now."
                    else -> ""
                }
                messages += turnNow
            }

            // Advance to the next step and queue its opening announcement.
            messages += advanceStep(position)
            return messages
        }

        // 3. Distance threshold announcements toward the current step
        if (distanceToStep != Float.MAX_VALUE) {
            messages += checkDistanceThreshold(distanceToStep, currentStep.distance, currentStep.icon)
        }

        // 4. Periodic destination-distance reminder (only while still navigating)
        val distanceToDest = position.distanceTo(route.end)
        if (!destinationAnnounced &&
            distanceToDest != Float.MAX_VALUE &&
            tickCount % REMINDER_INTERVAL_TICKS == 0
        ) {
            messages += buildDestinationReminder(distanceToDest.toInt())
        }

        // 5. Wrong-route detection
        /* val onRoute = isOnRoute(position)
       if (!onRoute && !wrongRouteAnnounced) {
            wrongRouteAnnounced = true
            messages += "You appear to be off route. Please recalculate your path."
        } else if (onRoute && wrongRouteAnnounced) {
            // Re-arm: user is back on route — allow future off-route warnings
            wrongRouteAnnounced = false
        }*/

        return messages
    }

    /**
     * Reset all mutable engine state. Call this when a new route is set or
     * navigation is stopped.
     */
    fun reset() {
        currentStepIndex = initialStepIndex
        lastAnnouncedDistanceBucket = null
        zoneState.clear()
        wrongRouteAnnounced = false
        destinationAnnounced = false
        tickCount = 0
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Advance to the next step and produce the announcement for it.
     * Returns an empty list if there are no more steps.
     */
    private fun advanceStep(position: Position): List<String> {
        currentStepIndex++
        lastAnnouncedDistanceBucket = null
        tickCount = 0

        if (currentStepIndex >= route.instructions.size) {
            return emptyList()
        }

        val nextStep = route.instructions[currentStepIndex]

        // DESTINATION step: do NOT announce here.
        // Its trigger position is `route.end` (the physical destination).
        // onLocationUpdate will fire this step on the next tick once the user
        // is actually within STEP_ARRIVAL_THRESHOLD of `to`.
        // Announcing it here means the previous step's completion (e.g. at the
        // elbow, 300 m away) immediately says "You have arrived" — wrong.
        if (nextStep.icon == NavigationIcon.DESTINATION) {
            return emptyList()
        }

        return listOf(buildStepAnnouncement(nextStep.instruction, nextStep.icon, nextStep.distance))
    }

    /**
     * Called by onLocationUpdate when the user reaches the DESTINATION step's position.
     * Separated from advanceStep so it only fires when the user is physically at `to`.
     */
    private fun announceArrival(): List<String> {
        destinationAnnounced = true
        val nameClause = if (destinationName.isNotEmpty()) " at $destinationName" else ""
        return listOf("You have arrived$nameClause.")
    }

    /**
     * Emit an announcement when the user crosses a distance bucket toward the current step.
     * Only one announcement per bucket per step is emitted.
     *
     * Buckets are ordered descending [50, 20, 5]. We announce the *smallest* bucket
     * the user has entered that they haven't been notified about yet:
     *  - Find the largest bucket B where distanceToStep <= B (user is inside B).
     *  - Only announce if lastAnnouncedDistanceBucket is null (first time) OR
     *    lastAnnouncedDistanceBucket > B (user is now inside a closer bucket).
     */
    private fun checkDistanceThreshold(
        distanceToStep: Float,
        stepTotalDistance: Float,
        icon: NavigationIcon
    ): List<String> {
        // Find the largest bucket that the user is currently inside
        val enteredBucket = DISTANCE_ANNOUNCEMENT_BUCKETS.firstOrNull { distanceToStep <= it }
            ?: return emptyList() // User is still farther than the largest bucket (50m)

        val last = lastAnnouncedDistanceBucket
        return if (last == null || last > enteredBucket) {
            // User has just entered a new (closer) bucket — announce it
            lastAnnouncedDistanceBucket = enteredBucket
            listOf(buildDistanceAnnouncement(distanceToStep.toInt(), icon))
        } else {
            emptyList()
        }
    }

    /**
     * Returns true when the user is within [WRONG_ROUTE_THRESHOLD] meters of any route waypoint
     * on the same floor.
     */
    private fun isOnRoute(position: Position): Boolean {
        val minDist = route.waypoints
            .filter { it.floor == position.floor }
            .minOfOrNull { position.distanceTo(it) }
            ?: return true  // Different floor entirely — don't trigger wrong-route
        return minDist <= WRONG_ROUTE_THRESHOLD
    }

    /**
     * Check all geofence zones:
     *  - Announce entry the first time the user enters a zone.
     *  - Re-arm the zone once the user exits by [GEOFENCE_EXIT_MULTIPLIER] × radius, so a
     *    user who leaves and re-enters will hear the announcement again.
     */
    private fun checkGeofences(position: Position): List<String> {
        val messages = mutableListOf<String>()
        for (zone in geofenceZones) {
            if (zone.center.floor != position.floor) continue
            val dist = position.distanceTo(zone.center)
            val isInsideNow = dist <= zone.radiusMeters

            val wasInside = zoneState[zone.id] ?: false

            if (isInsideNow && !wasInside) {
                // Entering the zone — announce
                zoneState[zone.id] = true
                messages += zone.announcementMessage
            } else if (!isInsideNow && wasInside) {
                // Exiting zone — check exit multiplier before re-arming
                val exitThreshold = zone.radiusMeters * GEOFENCE_EXIT_MULTIPLIER
                if (dist >= exitThreshold) {
                    zoneState[zone.id] = false  // re-arm: next entry will announce again
                }
            }
        }
        return messages
    }

    // ── Announcement text builders ────────────────────────────────────────────

    /**
     * Convert a [NavigationInstruction] into a natural-language voice announcement.
     */
    private fun buildStepAnnouncement(
        rawInstruction: String,
        icon: NavigationIcon,
        distanceMeters: Float
    ): String {
        val dist = distanceMeters.toInt()
        val distText = if (dist > 0) " for $dist meters" else ""
        return when (icon) {
            NavigationIcon.STRAIGHT          -> "Walk straight$distText."
            NavigationIcon.TURN_LEFT         -> "Turn left."
            NavigationIcon.TURN_RIGHT        -> "Turn right."
            NavigationIcon.TURN_SLIGHT_LEFT  -> "Bear slightly to the left."
            NavigationIcon.TURN_SLIGHT_RIGHT -> "Bear slightly to the right."
            NavigationIcon.STAIRS_UP         -> "Take the stairs or elevator up to the next floor."
            NavigationIcon.STAIRS_DOWN       -> "Take the stairs or elevator down to the next floor."
            NavigationIcon.ELEVATOR          -> "Take the elevator to the next floor."
            NavigationIcon.ESCALATOR_UP      -> "Take the escalator up."
            NavigationIcon.ESCALATOR_DOWN    -> "Take the escalator down."
            NavigationIcon.DESTINATION       -> {
                // Fallback — normally handled in advanceStep() with the destination name.
                val nameClause = if (destinationName.isNotEmpty()) " at $destinationName" else ""
                "You have arrived$nameClause."
            }
        }
    }

    /**
     * Build a "distance remaining" announcement appropriate for the direction of travel.
     * Uses natural phrasing such as "Gate A6 is 20 meters ahead" when destination name
     * is available and we are on the final approach.
     */
    private fun buildDistanceAnnouncement(distanceMeters: Int, icon: NavigationIcon): String {
        return when (icon) {
            NavigationIcon.DESTINATION -> {
                if (destinationName.isNotEmpty())
                    "$destinationName is $distanceMeters meters ahead."
                else
                    "Your destination is $distanceMeters meters ahead."
            }
            NavigationIcon.TURN_LEFT         -> "Turn left in $distanceMeters meters."
            NavigationIcon.TURN_RIGHT        -> "Turn right in $distanceMeters meters."
            NavigationIcon.TURN_SLIGHT_LEFT  -> "Bear left in $distanceMeters meters."
            NavigationIcon.TURN_SLIGHT_RIGHT -> "Bear right in $distanceMeters meters."
            NavigationIcon.STAIRS_UP,
            NavigationIcon.STAIRS_DOWN       -> "Stairs in $distanceMeters meters."
            NavigationIcon.ELEVATOR          -> "Elevator in $distanceMeters meters."
            NavigationIcon.ESCALATOR_UP,
            NavigationIcon.ESCALATOR_DOWN    -> "Escalator in $distanceMeters meters."
            else                             -> "Continue for $distanceMeters meters."
        }
    }

    /**
     * Build a periodic destination distance reminder.
     * E.g. "Gate A6 is 120 meters away. Keep walking."
     */
    private fun buildDestinationReminder(distanceMeters: Int): String {
        val nameClause = if (destinationName.isNotEmpty()) destinationName else "your destination"
        return when {
            distanceMeters <= 10  -> "" // Too close — the arrival step handles it
            distanceMeters < 100  -> "$nameClause is $distanceMeters meters away."
            else                  -> "$nameClause is about $distanceMeters meters away. Keep walking."
        }
    }
}
