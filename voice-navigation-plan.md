# Voice Navigation & Geofencing Plan

## Top-Level Overview

Add a custom voice navigation system and virtual geofencing to the existing `NavigationScreen`. The system will:

1. **Announce navigation instructions** via Android `TextToSpeech` — step-by-step guidance as the user moves through the airport ("Walk straight for 20 meters", "Turn left", "Gate A12 is 50 meters ahead", "You have reached your destination").
2. **Avoid repetition** — each unique message is only spoken once until conditions change (de-duplicate by tracking the last-spoken instruction).
3. **Detect wrong-route deviations** — if the user's position strays beyond a threshold from the expected route path, an announcement prompts them to recalculate.
4. **Virtual geofencing** — define circular zones around important airport locations (security, gates, lounges, restrooms, etc.) and trigger context-aware announcements when the user enters a zone.

No GPS / real location hardware is required. The system will hook into the existing `setCurrentLocation(position)` flow in `AirportNavigationViewModel` and work with the mock `Position` coordinate system already in place.

No new third-party dependencies are needed — Android `TextToSpeech` is part of the SDK.

---

## Architecture Diagram (concept)

```
User moves → setCurrentLocation(position) → AirportNavigationViewModel
                                                        │
                              ┌─────────────────────────┴──────────────────────┐
                              ▼                                                  ▼
                  VoiceNavigationEngine                           GeofenceManager
                  (detects step progress,                        (detects zone entry,
                   wrong-route, distance                          triggers zone
                   threshold announcements)                       announcements)
                              │                                          │
                              └──────────────────┬───────────────────────┘
                                                 ▼
                                     TextToSpeech (Android SDK)
                                     (speaks queued messages,
                                      de-duplicates)
```

---

## Sub-Tasks

---

### Sub-Task 1 — Add TextToSpeech Manager

**Intent**  
Encapsulate all Android `TextToSpeech` lifecycle and speech logic in a single, reusable class so the rest of the system only calls `speak(message)`. This ensures TTS is initialised once, properly torn down, and never repeats the same message back-to-back.

**Expected Outcomes**
- A new file `VoiceSpeaker.kt` exists in `ui/navigation/` (or `util/`).
- Calling `speak("Turn left")` queues the utterance via TTS.
- Calling `speak("Turn left")` a second time in a row is silently ignored (de-duplicate).
- `shutdown()` properly releases TTS resources.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/navigation/VoiceSpeaker.kt`.
2. Implement `VoiceSpeaker(context: Context)` with an `OnInitListener` that sets a ready flag.
3. Store `lastSpokenMessage: String` to avoid repeating the same line.
4. `fun speak(message: String, flushQueue: Boolean = false)` — skips if `message == lastSpokenMessage`; otherwise calls `tts.speak(...)` with `TextToSpeech.QUEUE_FLUSH` or `QUEUE_ADD`.
5. `fun shutdown()` — calls `tts.stop()` then `tts.shutdown()`.
6. No Android permissions are required for TTS.

**Relevant Context**
- `android.speech.tts.TextToSpeech` — built-in Android SDK, no Gradle dependency needed.
- Pattern matches existing single-responsibility classes in the project.

**Status** — `[ ] pending`

---

### Sub-Task 2 — Define Geofence Zones Model & Data

**Intent**  
Define a `GeofenceZone` data model and populate mock zones for the terminal's important locations (security checkpoint, gates, lounges, restrooms, etc.) that map to the existing `Position` coordinate system and `AmenityType` enum already in `AirportModels.kt`.

**Expected Outcomes**
- A `GeofenceZone` data class exists in `AirportModels.kt` (or a new models file).
- A set of hardcoded demo zones is available in `MockData` or a companion object, using coordinates that match the mock terminal's existing amenity/gate positions.
- Each zone has: an id, a centre `Position`, a radius in meters, a human-readable `announcementMessage`, and a `zoneType` tag.

**Todo List**
1. Add `data class GeofenceZone(id, name, center: Position, radiusMeters: Float, announcementMessage: String, type: GeofenceZoneType)` to `AirportModels.kt`.
2. Add `enum class GeofenceZoneType { SECURITY, GATE, LOUNGE, RESTROOM, FOOD_COURT, BOARDING, ARRIVAL, CUSTOM }`.
3. Add `fun getSampleGeofenceZones(): List<GeofenceZone>` to `MockData.kt` — create zones centred on existing mock gate/amenity positions with radii ~20–30 meters.
4. Example announcements: "Security checkpoint ahead.", "You have arrived at Gate A12.", "Lounge entrance on your left.", "Restrooms are nearby."

**Relevant Context**
- Existing `AmenityType` enum in [`AirportModels.kt`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt).
- Existing mock amenity positions in [`MockData.kt`](app/src/main/java/com/example/flightbooking/data/MockData.kt) — use these same coordinates so zones overlap real mock data.
- `Position.distanceTo()` already implements the Euclidean distance needed for zone-entry checks.

**Status** — `[ ] pending`

---

### Sub-Task 3 — Build VoiceNavigationEngine

**Intent**  
Create the core logic layer that, given the user's current `Position`, the active `NavigationRoute`, and the list of `GeofenceZone`s, decides *what* to say and *when*. This is the brain of the system — it must handle step advancement, distance threshold announcements, wrong-route detection, and geofence entry.

**Expected Outcomes**
- A new `VoiceNavigationEngine.kt` class (no Android framework dependency — pure Kotlin logic, easily testable).
- Calling `onLocationUpdate(position)` produces zero or more voice messages, returned as a `List<String>` (or via callback).
- Duplicate suppression is delegated to `VoiceSpeaker` — the engine may emit the same message type but the speaker won't repeat it.
- "Wrong route" fires when user's position deviates > `WRONG_ROUTE_THRESHOLD` (e.g., 50 meters) from the nearest waypoint on the route.
- Step auto-advance: when user passes within `STEP_ARRIVAL_THRESHOLD` (e.g., 10 meters) of the current step's `position`, move to the next step and announce it.
- Distance threshold announcements: at 50 m, 20 m, 5 m from the current step's target position, announce remaining distance.
- Geofence entry: compare current position against all active zones; when entering (crossing inside radius), emit the zone's announcement.

**Instruction generation examples**
- Step with `NavigationIcon.TURN_LEFT` → "Turn left."
- Step with `NavigationIcon.STRAIGHT`, distance 20 m → "Walk straight for 20 meters."
- Approaching gate → "Gate A12 is 50 meters ahead."
- Final step (`NavigationIcon.DESTINATION`) → "You have reached your destination."
- Wrong route → "You appear to be off-route. Recalculating."
- Geofence SECURITY → "Security checkpoint ahead."
- Geofence GATE → "You have arrived at Gate A12."

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/navigation/VoiceNavigationEngine.kt`.
2. Constructor takes: `route: NavigationRoute`, `geofenceZones: List<GeofenceZone>`.
3. Maintain mutable state: `currentStepIndex`, `lastAnnouncedDistanceBucket`, `enteredZoneIds: MutableSet<String>`, `wrongRouteAnnounced: Boolean`.
4. Implement `fun onLocationUpdate(position: Position): List<String>` — returns list of messages to speak.
5. Step-advance logic: if `position.distanceTo(currentStep.position) < STEP_ARRIVAL_THRESHOLD`, advance step and return next step announcement.
6. Distance bucket logic: calculate distance to current step target; announce at 50 m, 20 m, 5 m buckets; store last bucket to avoid re-announcing.
7. Wrong-route check: find the minimum distance from `position` to any waypoint in `route.waypoints`; if > `WRONG_ROUTE_THRESHOLD` and not already announced, return wrong-route message and set flag.
8. Geofence check: for each zone not in `enteredZoneIds`, if `position.distanceTo(zone.center) <= zone.radiusMeters`, add to set and return zone announcement.
9. Expose `fun reset()` to clear all state (called when route changes).
10. Define constants: `STEP_ARRIVAL_THRESHOLD = 10f`, `WRONG_ROUTE_THRESHOLD = 50f`, distance buckets `[50f, 20f, 5f]`.

**Relevant Context**
- `NavigationInstruction.position`, `.distance`, `.icon` in [`AirportModels.kt`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt:146).
- `NavigationRoute.waypoints` in [`AirportModels.kt`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt:134).
- `Position.distanceTo()` at [`AirportModels.kt:52`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt:52).

**Status** — `[ ] pending`

---

### Sub-Task 4 — Integrate Voice Navigation into AirportNavigationViewModel

**Intent**  
Wire `VoiceNavigationEngine` and `VoiceSpeaker` into `AirportNavigationViewModel` so that every time `setCurrentLocation()` is called, the engine evaluates the position and the speaker announces any resulting messages. The ViewModel holds the engine's lifecycle; the speaker is passed in (or created) at construction time.

**Expected Outcomes**
- `AirportNavigationViewModel` creates/holds a `VoiceNavigationEngine` instance whenever a new route is set.
- `setCurrentLocation()` calls `engine.onLocationUpdate(position)` and passes results to `VoiceSpeaker.speak()`.
- When `clearNavigation()` is called, the engine is reset and TTS is silenced.
- `AirportNavigationState` gains a new `currentStepIndex: Int` field (driven by the engine's step-advance) so the UI step card stays in sync.
- The ViewModel exposes `fun initVoiceSpeaker(context: Context)` (called from the Composable's `DisposableEffect`) and `fun shutdownVoiceSpeaker()`.

**Todo List**
1. Add `currentStepIndex: Int = 0` to `AirportNavigationState` in `AirportModels.kt`.
2. In `AirportNavigationViewModel`: add `private var voiceSpeaker: VoiceSpeaker? = null` and `private var voiceEngine: VoiceNavigationEngine? = null`.
3. Add `fun initVoiceSpeaker(context: Context)` — instantiates `VoiceSpeaker`.
4. Add `fun shutdownVoiceSpeaker()` — calls `voiceSpeaker?.shutdown()`.
5. In `setDestination()` — after building the route, instantiate `VoiceNavigationEngine(route, geofenceZones)` and call `voiceSpeaker?.speak("Navigation started. ${instructions[0].instruction}")`.
6. In `setCurrentLocation()` — after updating state, call `voiceEngine?.onLocationUpdate(position)?.forEach { msg -> voiceSpeaker?.speak(msg) }`.
7. If engine advances step index, update `_navigationState.value.currentStepIndex` accordingly.
8. In `clearNavigation()` — call `voiceEngine?.reset()`, nullify engine ref, call `voiceSpeaker?.speak` nothing (silent).
9. Load geofence zones from `MockData.getSampleGeofenceZones()` in `loadAirportData()` and store in state or a ViewModel field.
10. Override `onCleared()` to call `shutdownVoiceSpeaker()`.

**Relevant Context**
- `setCurrentLocation()` at [`AirportNavigationViewModel.kt:59`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:59).
- `setDestination()` at [`AirportNavigationViewModel.kt:71`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:71).
- `clearNavigation()` at [`AirportNavigationViewModel.kt:98`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:98).

**Status** — `[ ] pending`

---

### Sub-Task 5 — Update NavigationScreen UI

**Intent**  
Update `NavigationScreen.kt` to:
1. Initialise and shut down the `VoiceSpeaker` via `DisposableEffect`.
2. Reflect the ViewModel-driven `currentStepIndex` (instead of local `remember` state) so the voice-advanced step and UI step are always in sync.
3. Add a mute/unmute toggle button in the top bar so the user can silence voice without leaving the screen.

**Expected Outcomes**
- `NavigationScreen` calls `viewModel.initVoiceSpeaker(context)` on entry and `viewModel.shutdownVoiceSpeaker()` on exit.
- The current step card always shows the step the engine has advanced to.
- A speaker icon button in the top bar toggles mute state stored in the ViewModel (or local state).
- No Accessibility/TalkBack APIs are touched.

**Todo List**
1. In `NavigationScreen`, replace `var currentStepIndex by remember { mutableStateOf(0) }` with `val currentStepIndex = navigationState.currentStepIndex`.
2. Add `val context = LocalContext.current` and a `DisposableEffect(Unit)` that calls `viewModel.initVoiceSpeaker(context)` on enter and `viewModel.shutdownVoiceSpeaker()` on dispose.
3. Add `isMuted: Boolean = false` to `AirportNavigationState` and a `fun toggleMute()` function in the ViewModel that sets it; `VoiceSpeaker.speak()` checks this flag via a `isMuted` property.
4. Add a speaker/mute `IconButton` to the `TopAppBar` actions area.
5. Keep Previous/Next manual controls fully functional (they just update `currentStepIndex` in state via a new ViewModel method `fun jumpToStep(index: Int)`).

**Relevant Context**
- `NavigationScreen` at [`NavigationScreen.kt:42`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:42).
- `CurrentStepCard` at [`NavigationScreen.kt:287`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:287).
- `NavigationControls` at [`NavigationScreen.kt:344`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:344).

**Status** — `[ ] pending`

---

### Sub-Task 6 — Add Manifest Permissions

**Intent**  
Add the required permission declarations to `AndroidManifest.xml`. Android `TextToSpeech` does not need a permission, but it is good practice to declare `FOREGROUND_SERVICE` if voice is expected while screen is off. No location permissions are needed because the system works with mock/simulated coordinates only (no GPS).

**Expected Outcomes**
- `AndroidManifest.xml` declares no new location permissions (mock-only system).
- No changes needed beyond possibly a comment clarifying TTS requires no permission.
- If in future GPS is added, `ACCESS_FINE_LOCATION` would go here.

**Todo List**
1. Open `AndroidManifest.xml` and confirm no new permissions are needed for the mock system.
2. Add a comment noting that real GPS integration would require `ACCESS_FINE_LOCATION`.
3. No structural changes needed.

**Relevant Context**
- [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml)

**Status** — `[ ] pending`

---

### Sub-Task 7 — Demo / Simulation Mode

**Intent**
Add a "Walk Demo" mode in the ViewModel that automatically simulates the user walking the route step-by-step using a coroutine timer. Each tick advances the simulated position along the route waypoints, calling `setCurrentLocation()` internally so the voice engine and UI both respond exactly as they would with real movement. This lets the full voice + geofence system be exercised without any external location input.

**Expected Outcomes**
- A "Start Demo" / "Stop Demo" button appears on `NavigationScreen` when a route is active.
- Tapping "Start Demo" begins automatic position simulation: the mock user moves from the route start toward each waypoint at a configurable tick interval (e.g., every 1.5 seconds, advancing ~5 meters per tick).
- Voice announcements fire naturally as thresholds are crossed — no special wiring needed beyond calling `setCurrentLocation()`.
- "Stop Demo" halts the simulation coroutine; the user's position stays at the last simulated point.
- `isDemoRunning: Boolean = false` is added to `AirportNavigationState`.

**Todo List**
1. Add `isDemoRunning: Boolean = false` to `AirportNavigationState` in `AirportModels.kt`.
2. In `AirportNavigationViewModel`, add `private var demoJob: Job? = null`.
3. Implement `fun startDemo()`:
   - Set `isDemoRunning = true` in state.
   - Launch a `viewModelScope` coroutine.
   - Interpolate positions linearly between each consecutive pair of waypoints in `navigationRoute.waypoints`.
   - Every tick (1500ms delay), call `setCurrentLocation(interpolatedPosition)`.
   - Stop automatically when the final waypoint is reached.
4. Implement `fun stopDemo()`:
   - Cancel `demoJob`, set `isDemoRunning = false`.
5. In `clearNavigation()`, also call `stopDemo()`.
6. In `NavigationScreen`, observe `isDemoRunning` from state.
7. Add a "▶ Walk Demo" / "⏹ Stop Demo" `OutlinedButton` below `NavigationControls`, visible only when a route is active.

**Relevant Context**
- `navigationRoute.waypoints` in `AirportModels.kt` — currently only contains start + end; the `calculateRoute()` method in the ViewModel can be extended to include intermediate points if more granular simulation is desired (optional refinement).
- `viewModelScope` is already used for async work in [`AirportNavigationViewModel.kt:30`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:30).
- `setCurrentLocation()` at [`AirportNavigationViewModel.kt:59`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:59) — this is the only call needed per simulation tick.

**Status** — `[ ] pending`

---

## File Summary

| File | Action |
|------|--------|
| `app/src/main/java/com/example/flightbooking/navigation/VoiceSpeaker.kt` | **Create** |
| `app/src/main/java/com/example/flightbooking/navigation/VoiceNavigationEngine.kt` | **Create** |
| `app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt` | **Modify** — add `GeofenceZone`, `GeofenceZoneType`, `currentStepIndex`, `isMuted`, `isDemoRunning` |
| `app/src/main/java/com/example/flightbooking/data/MockData.kt` | **Modify** — add `getSampleGeofenceZones()` |
| `app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt` | **Modify** — wire engine + speaker, demo coroutine |
| `app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt` | **Modify** — DisposableEffect, mute toggle, state-driven step index, demo button |
| `app/src/main/AndroidManifest.xml` | **Minor** — add comment only |

---

## Key Design Decisions

- **No GPS / no Play Services location** — the voice system is fully driven by `setCurrentLocation(position)` which works with mock coordinates. Real GPS integration is a future concern.
- **No new Gradle dependencies** — `TextToSpeech` is Android SDK. No library additions needed.
- **Engine is pure Kotlin** — `VoiceNavigationEngine` has zero Android imports; it can be unit tested without a device.
- **Mute toggle lives in ViewModel state** — survives recomposition and can be persisted later.
- **Geofencing is virtual / software-only** — no `GeofencingClient` from Play Services; all checks are Euclidean distance comparisons inside the engine.
