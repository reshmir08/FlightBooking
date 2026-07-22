# ✈️ Flight Booking App

A modern Android flight booking and **voice-assisted indoor airport navigation** app built with Jetpack Compose.

---

## 📱 Features

### 🎫 Flight Booking
- **Trip Type Selection** — One-way, Round-trip, and Multi-city
- **Airport Search** — Departure and arrival airport selection
- **Date Picker** — Native Android DatePickerDialog
- **Passenger & Cabin Class** — Adults, children, Economy / Premium Economy / Business / First Class
- **Flight Results** — Filterable and sortable list with pricing and timing
- **Seat Selection** — Interactive seat map
- **Passenger Form** — Enter traveller details
- **Payment** — Secure payment interface
- **Booking Confirmation** — Reference number and itinerary summary

---

### 🗺️ Voice-Assisted Indoor Airport Navigation

The centrepiece feature: a fully custom, GPS-free indoor navigation system with real-time **Text-To-Speech voice guidance**, virtual **geofencing**, and a procedurally rendered 2D terminal map — all built without any Accessibility / TalkBack API.

#### 🔊 Custom Voice Navigation (Android TextToSpeech)

Voice guidance is implemented entirely via `android.speech.tts.TextToSpeech` — no third-party libraries.

**`VoiceSpeaker`** (`navigation/VoiceSpeaker.kt`)
- Wraps `TextToSpeech` with async-safe initialisation
- `pendingMessage` buffer — messages queued before TTS is ready are replayed automatically on init
- Back-to-back de-duplication — same message never spoken twice in a row
- Mute toggle — suppresses all speech without destroying the TTS engine
- Configurable `speechRate` and `pitch` (range 0.5 × – 2.0 ×)
- `QUEUE_FLUSH` / `QUEUE_ADD` modes for urgent vs queued announcements
- Clean `shutdown()` called via `DisposableEffect` when the screen is disposed

**`VoiceNavigationEngine`** (`navigation/VoiceNavigationEngine.kt`)
- Pure Kotlin — zero Android imports; fully unit-testable
- Driven by `onLocationUpdate(position)` returning a `List<String>` of messages to speak
- **Announcement priority order** on every tick:
  1. Geofence zone entries
  2. Step arrival / turn-now
  3. Distance threshold buckets (50 m → 20 m → 5 m)
  4. Periodic destination reminder (every 20 ticks)
  5. Wrong-route warning

#### 📣 Voice Announcement Examples

| Situation | Announcement |
|---|---|
| Navigation starts | *"Navigation started. Gate A6 is 500 meters away. Head west and walk 200 meters."* |
| Approaching a turn at 50 m | *"Turn right in 50 meters."* |
| Approaching a turn at 20 m | *"Turn right in 20 meters."* |
| Exactly at the corner | *"Turn right now."* → *"Walk straight for 300 meters."* |
| Final approach at 20 m | *"Gate A6 is 20 meters ahead."* |
| Destination reached | *"You have arrived at Gate A6."* |
| Periodic reminder | *"Gate A6 is about 120 meters away. Keep walking."* |
| Wrong route | *"You appear to be off route. Please recalculate your path."* |

#### 📐 Distance Threshold System

The engine watches three distance buckets per step. Each bucket fires **once per step** — once crossed, it never repeats for the same step:

| Distance to next waypoint | Announcement fired |
|---|---|
| First time ≤ 50 m | *"Turn right in 50 meters."* |
| First time ≤ 20 m | *"Turn right in 20 meters."* |
| First time ≤ 5 m | *"Turn right in 5 meters."* |
| ≤ 10 m (arrival threshold) | *"Turn right now."* + step advance |

#### 🛡️ Step-Arrival Guard

A `arrivedAtCurrentStepOnAdvance` flag prevents the engine from immediately triggering arrival the moment a step is loaded (e.g. navigation just started, and the user is already standing at `from`). Arrival only fires after the user has moved **away** from a waypoint and returns to within 10 m — guaranteeing the correct sequence:

```
Start → "Head west…" (spoken once) → user walks → corner reached → "Turn right now." → walks → destination → "You have arrived."
```

#### 📍 Virtual Geofencing

The app defines **virtual geofence zones** as logical circles around airport locations. When the user enters a zone, a context-aware voice announcement fires exactly once per visit. Zones re-arm after the user exits by 2× the zone radius.

**Zone types and colour coding on the map:**

| Type | Colour | Example announcement |
|---|---|---|
| 🔴 Security | Red | *"Security checkpoint ahead. Please have your boarding pass and ID ready."* |
| 🔵 Gate | Blue | *"You have arrived at Gate A1."* |
| 🟢 Boarding | Green | *"You have arrived at Gate A6. Boarding is in progress."* |
| 🟣 Lounge | Purple | *"Maple Leaf Lounge entrance is nearby on your left."* |
| 🔵 Restroom | Teal | *"Restrooms are on your right."* |
| 🟠 Food Court | Orange | *"Food court ahead. Multiple dining options available."* |
| ⚫ Custom | Grey | *"Starbucks coffee shop is on your right."* |

**Defined zones include:** Security Checkpoint, Gates A1 / A4 / A6, Gates B1 / C1, Maple Leaf Lounge, Air Canada Signature Suite, Restrooms (Gate A & B areas), Food Court, Starbucks — all centred on real mock positions so they fire naturally during a Demo Walk.

---

#### 🗺️ 2D Indoor Map

The terminal floor plan is **procedurally rendered on Canvas** — no image assets required.

**Map layers (bottom → top):**

| Layer | File | Description |
|---|---|---|
| 1 | `IndoorMapCanvas.kt` | Terminal background, corridors, room outlines, labels |
| 2 | `GeofenceOverlay.kt` | Animated dashed zone rings, colour-coded by type |
| 3 | `RoutePathRenderer.kt` | Blue animated-dash route polyline with chevron arrows |
| 4 | `PoiMarkersOverlay.kt` | Gate dots (colour = boarding status) + amenity dots |
| 5 | `UserLocationMarker.kt` | Pulsing blue dot + directional arrow |

**Map features:**
- Pinch-to-zoom (0.6 × – 4.0 ×) and single-finger pan
- Re-centre FAB snaps viewport back to user position
- Coordinate system: logical units ≈ 1 metre; `x` = east, `y` = south
- Auto-switches visible floor when route crosses a floor boundary

**Coordinate system:**
```
x: 60 – 620  (west → east)
y: 120 – 480 (north → south)
1 logical unit ≈ 1 metre
```

---

#### 🧭 Navigation Screen Layout

```
┌──────────────────────────────────────────┐
│  TopAppBar (title · stop · steps · mute) │
├──────────────────────────────────────────┤
│  TopInstructionCard                       │
│  ┌────────────────────────────────────┐  │
│  │ [Icon]  Step instruction text  1/3 │  │
│  │         Distance: 200 m            │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌── 2D Indoor Map ─────────────────┐   │
│  │  Floor plan + Geofence rings      │   │
│  │  Route polyline + Chevrons        │   │
│  │  Gate / Amenity markers           │   │
│  │  User location dot + arrow        │   │
│  │                       [⊙ Re-centre│   │
│  │  [▶ Demo Walk]                    │   │
│  └───────────────────────────────────┘  │
│                                          │
│  ┌── Static Compass Rose ─────────────┐ │
│  │   N (red) · E · S · W (grey)       │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌── BottomInfoBar ───────────────────┐ │
│  │  Distance: 500m  │  ETA: 6 min  🔊 │ │
│  │  [← Prev]  [⊙ Recalc]  [Next →]   │ │
│  └────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

**Navigation Screen features:**
- **TopInstructionCard** — current step instruction with directional icon and distance
- **Static Compass Rose** — fixed N/E/S/W orientation aid below the map (never rotates)
- **BottomInfoBar** — live distance and walking-time ETA, mute toggle, Prev/Recalc/Next controls
- **All Steps Sheet** — `ModalBottomSheet` listing every step, tap any to jump to it
- **Demo Walk FAB** — simulates a walk along the full route at 10 m/tick, triggering all voice and geofence events naturally

---

#### 📡 Route Calculation

Routes are **L-shaped** — horizontal leg first (east/west), then vertical leg (north/south).

**Distance formula:** Manhattan distance `|Δx| + |Δy|` (not Euclidean). This correctly reflects the actual corridor path walked:
```
200 m west + 300 m south = 500 m total  ✅
sqrt(200² + 300²) ≈ 360 m (wrong — diagonal through walls)  ❌
```

**Instruction steps generated:**

| Step | Trigger position | Example |
|---|---|---|
| 1 | `from` (start) | *"Head west and walk 200 meters."* |
| 2 | `elbow` (corner) | *"Turn right and walk 300 meters."* |
| Last | `to` (destination) | *"You have arrived at Gate A6."* |

**Turn direction logic:**
```
turnLeft = (deltaX > 0) != (deltaY > 0)
```
- Heading east (`Δx > 0`) + turning south (`Δy > 0`) → **RIGHT**
- Heading east (`Δx > 0`) + turning north (`Δy < 0`) → **LEFT**
- Heading west (`Δx < 0`) + turning north (`Δy < 0`) → **RIGHT**
- Heading west (`Δx < 0`) + turning south (`Δy > 0`) → **LEFT**

---

### 🏢 Airport Data (Terminal 1 Mock)

**Gates:** A1 – A10 (y=150) and B1 – B10 (y=350), Floor 1

**Gate statuses:** `BOARDING` (green) · `AVAILABLE` (grey) · `DELAYED` (orange) · `DEPARTED` (dark grey) · `CANCELLED` (red) · `MAINTENANCE` (yellow)

**Amenities include:** Restrooms · Starbucks · Tim Hortons · Food Court · Maple Leaf Lounge · Air Canada Signature Suite · Charging Stations · ATM · Currency Exchange · Duty Free · Pharmacy · Medical Center · Information Desk · Prayer Room · Nursing Room · Kids Play Area

---

### 👤 Profile & Settings
- **User Profile** — Personal details, Aeroplan membership, travel preferences, saved payment methods, booking history
- **Settings** — Notifications, language, theme, privacy, help & support

---

### 🎨 UI / UX
- **Material Design 3** — `Scaffold`, `TopAppBar`, `Card`, `ModalBottomSheet`, `ExtendedFloatingActionButton`
- **Animations** — Pulsing user marker, animated route dash, step pulse on instruction change
- **Bottom Navigation** — Book · Airport Nav · Profile · Settings

---

## 🏗️ Technical Architecture

### Technology Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose (BOM 2024.09.00) |
| Architecture | MVVM |
| State | `StateFlow` + Compose `collectAsState` |
| Navigation | Jetpack Navigation Compose 2.7.7 |
| Voice | `android.speech.tts.TextToSpeech` (no third-party SDK) |
| Map rendering | Compose `Canvas` (procedural — no image assets) |
| Minimum SDK | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |
| Build | Gradle 8 + Kotlin 2.0.21 |

### Project Structure

```
app/src/main/java/com/example/flightbooking/
│
├── data/
│   ├── MockData.kt                  ← Sample gates, amenities, geofence zones
│   ├── MockFloorPlanData.kt         ← Procedural floor plan (corridors + rooms)
│   └── models/
│       ├── AirportModels.kt         ← Terminal, Gate, Amenity, NavigationRoute,
│       │                               GeofenceZone, DistanceInfo, NavigationIcon, …
│       ├── IndoorMapModels.kt       ← FloorPlan, Corridor, RoomOutline, MapTransform
│       ├── BookingModels.kt
│       └── FlightModels.kt
│
├── navigation/
│   ├── VoiceSpeaker.kt              ← Android TTS wrapper (de-dup, mute, pending buffer)
│   ├── VoiceNavigationEngine.kt     ← Pure-Kotlin step engine (geofences, thresholds, guard)
│   ├── MapCoordinates.kt            ← Logical ↔ canvas coordinate math, pinch/pan helpers
│   └── NavGraph.kt                  ← Jetpack Navigation graph
│
├── ui/
│   ├── map/
│   │   ├── IndoorMapView.kt         ← Layer stack + gesture handler
│   │   ├── IndoorMapCanvas.kt       ← Floor plan background renderer
│   │   ├── GeofenceOverlay.kt       ← Animated dashed zone rings
│   │   ├── RoutePathRenderer.kt     ← Animated-dash polyline + chevrons
│   │   ├── PoiMarkersOverlay.kt     ← Gate/amenity dot markers + tap detection
│   │   ├── UserLocationMarker.kt    ← Pulsing dot + heading arrow
│   │   └── FloorSelector.kt        ← Floor chip row (hidden when ≤ 1 floor)
│   │
│   ├── screens/
│   │   ├── NavigationScreen.kt      ← Full navigation UI (map, HUD, compass, demo)
│   │   ├── GateFinderScreen.kt      ← Gate search → triggers navigation
│   │   ├── AirportHubScreen.kt
│   │   ├── AmenitiesListScreen.kt
│   │   ├── TerminalMapScreen.kt
│   │   ├── BookingScreen.kt
│   │   ├── FlightResultsScreen.kt
│   │   ├── FlightDetailsScreen.kt
│   │   ├── PassengerFormScreen.kt
│   │   ├── PaymentScreen.kt
│   │   ├── BookingConfirmationScreen.kt
│   │   ├── ProfileScreen.kt
│   │   └── SettingsScreen.kt
│   │
│   ├── components/                  ← Reusable Compose components
│   └── theme/                       ← Color.kt · Theme.kt · Type.kt
│
└── viewmodel/
    ├── AirportNavigationViewModel.kt ← Navigation state, TTS lifecycle, route calc,
    │                                    demo walk, geofence zone tracking
    ├── BookingViewModel.kt
    └── FlightSearchViewModel.kt
```

### Key Classes

#### `AirportNavigationViewModel`
- Owns `VoiceSpeaker` lifecycle (init / shutdown via `DisposableEffect`)
- Calls `VoiceNavigationEngine.onLocationUpdate()` on every position tick
- Calculates L-shaped routes with Manhattan distance
- Tracks `activeGeofenceZones` set for map pulsing (independent of TTS engine)
- `pendingStartAnnouncement` — replays start message if TTS wasn't ready at route set time
- `startDemo()` — coroutine that interpolates positions along route waypoints at 10 m / 1 000 ms ticks

#### `VoiceNavigationEngine`
- Zero Android dependencies — pure Kotlin, fully testable
- `arrivedAtCurrentStepOnAdvance` guard — prevents false step-arrival on navigation start
- Distance bucket system: fires once per threshold (50 m / 20 m / 5 m) per step
- Geofence state map: per-zone `Boolean` with 2× exit multiplier for re-arming
- `reset()` clears all state for route recalculation

#### `VoiceSpeaker`
- Async TTS init with `pendingMessage` buffer
- `speak(message, flushQueue, force)` — de-duplication, mute check, queue mode
- `resetLastMessage()` — clears dedup cache on route change

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Meerkat or later
- JDK 17+
- Android SDK 36
- Gradle 8.13+

### Build & Run

```bash
# Build
./gradlew build

# Install on connected device / emulator
./gradlew installDebug
```

### Try the Navigation System

1. Launch the app and tap **Airport Nav** in the bottom navigation bar
2. Tap **Gate Finder** and search for any gate (e.g. `A6`, `gate b1`, `B10`)
3. Tap **Navigate** — the Navigation Screen opens with voice announcing the first step
4. Tap **▶ Demo Walk** to simulate a walk along the route
5. Listen for voice announcements at each distance threshold and at every geofence zone
6. Tap the **ℹ️** icon in the top bar to see all steps; tap any step to jump to it
7. Tap **🔊** to toggle mute; tap **⊙ Recalc** to recalculate from current position

---

## 📋 All Screens

| # | Screen | Description |
|---|---|---|
| 1 | Booking | Main flight search |
| 2 | Flight Results | Available flights list |
| 3 | Flight Details | Single flight detail |
| 4 | Seat Selection | Interactive seat map |
| 5 | Passenger Form | Traveller details |
| 6 | Payment | Payment processing |
| 7 | Booking Confirmation | Confirmation + reference |
| 8 | Airport Hub | Central airport services hub |
| 9 | Gate Finder | Gate search → starts navigation |
| 10 | Terminal Map | Static overview map |
| 11 | **Navigation** | **Full voice-assisted navigation** |
| 12 | Amenities List | Browse airport facilities |
| 13 | Profile | User profile & preferences |
| 14 | Settings | App settings |

---

## 🔧 Customisation

| What to change | Where |
|---|---|
| Colours & theme | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Gate & amenity positions | `data/MockData.kt` → `getSampleGates()`, `getSampleAmenities()` |
| Geofence zones | `data/MockData.kt` → `getSampleGeofenceZones()` |
| Floor plan corridors & rooms | `data/MockFloorPlanData.kt` |
| Voice announcement text | `navigation/VoiceNavigationEngine.kt` → `buildStepAnnouncement()`, `buildDistanceAnnouncement()` |
| Distance thresholds | `VoiceNavigationEngine.DISTANCE_ANNOUNCEMENT_BUCKETS` |
| Demo walk speed | `AirportNavigationViewModel.DEMO_TICK_MS`, `DEMO_STEP_METERS` |
| TTS speech rate / pitch | `VoiceSpeaker.speechRate`, `VoiceSpeaker.pitch` |
| Navigation routes | `navigation/NavGraph.kt` |
| String resources | `res/values/strings.xml` |

---

## 📝 Notes

- This app uses **no GPS or Google Play Services** — all positioning is via mock coordinates and the Demo Walk simulation
- **No TalkBack / Accessibility API** is used — voice guidance is entirely custom via `android.speech.tts.TextToSpeech`
- This is a demonstration app and does not process real flight bookings or payments

---

Built with ❤️ using Jetpack Compose and modern Android development practices.
