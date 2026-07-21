# 2D Indoor Map Navigation Plan

## Top-Level Overview

Replace the existing `RouteMapCard` Canvas widget in `NavigationScreen.kt` with a full
2D interactive indoor map experience. The floor plan is rendered procedurally in Compose
Canvas from the existing mock gate and amenity positions — no image assets required. The
route, user marker, POI markers and direction arrows are overlaid on top. Pan and pinch-zoom
gestures are supported. Voice navigation (TTS) is unchanged.

No new Gradle dependencies are needed. Everything is implemented using Compose Canvas,
`Animatable`, `detectTransformGestures`, and `detectDragGestures` — all already available
through the existing Compose BOM.

### Coordinate System

The mock data lives in a logical coordinate space:
- x: 80 – 600 (corridors run horizontally)
- y: 150 – 450 (gate rows at fixed y values)
- floor: 1 or 2

A single `MapTransform` holds the current pan offset and zoom scale. Every logical
`Position(x, y)` is converted to canvas pixels by:

```
canvasX = (x - viewportLeft) * effectiveScale
canvasY = (y - viewportTop) * effectiveScale
```

where `effectiveScale = baseScale * zoom` and `baseScale` is computed once when the
map is first laid out to fit the whole terminal in the canvas at zoom=1.

---

## Sub-Tasks

---

### Sub-Task 1 — Map Data Models

**Intent**
Add lightweight data models that describe the floor plan geometry (corridors, rooms, zones)
and a `MapTransform` state class used by the UI. These models are pure data — no Android
imports — making them easily testable and reusable.

**Expected Outcomes**
- A new file `IndoorMapModels.kt` in `data/models/`.
- `FloorPlan` model holds corridor segments, room outlines, and zone rectangles for one
  floor, all expressed in the existing logical coordinate space.
- `MapTransform` holds zoom (Float) and pan offset (Offset) and exposes
  `toCanvas(position, canvasSize)` for converting a `Position` to canvas pixels.
- `AirportNavigationState` gains `mapTransform: MapTransform` and `visibleFloor: Int`.
- `MockFloorPlanData` object provides `getFloorPlan(floor: Int): FloorPlan` using the
  mock gate/amenity positions already in `MockData`.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/data/models/IndoorMapModels.kt`.
2. Define `data class Corridor(val start: Position, val end: Position, val widthMeters: Float = 4f)`.
3. Define `data class RoomOutline(val id: String, val label: String, val bounds: RectF, val floor: Int, val type: RoomType)` where `RectF` is `android.graphics.RectF` — or use a plain `data class MapRect(left, top, right, bottom)` to keep it Android-free.
4. Define `enum class RoomType { GATE, LOUNGE, CAFE, RESTROOM, FOOD_COURT, SECURITY, GENERAL }`.
5. Define `data class FloorPlan(val floor: Int, val corridors: List<Corridor>, val rooms: List<RoomOutline>)`.
6. Define `data class MapTransform(val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f)`.
7. Add `mapTransform: MapTransform = MapTransform()` and `visibleFloor: Int = 1` to `AirportNavigationState`.
8. Create `app/src/main/java/com/example/flightbooking/data/MockFloorPlanData.kt`.
9. In `MockFloorPlanData`, implement `getFloorPlan(floor: Int): FloorPlan`:
   - Floor 1 corridors: main horizontal spine y=250 (x=80→600), gate wing A (y=200→150, x=80→550), gate wing B (y=300→350, x=80→550), vertical connector at x=300 (y=150→350).
   - Floor 2 corridors: lounge concourse y=200 (x=200→600), gate row C y=200 (x=200→600).
   - Rooms: one `RoomOutline` per gate (small rectangle centered on gate position), plus larger zones for security, food court, and lounges derived from amenity positions in `MockData`.

**Relevant Context**
- [`AirportModels.kt`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt) — existing `Position`, `AirportNavigationState`
- [`MockData.kt`](app/src/main/java/com/example/flightbooking/data/MockData.kt:185) — gate/amenity positions to derive room outlines from

**Status** — `[ ] pending`

---

### Sub-Task 2 — MapTransform & Coordinate Utilities

**Intent**
Provide a single place for all coordinate math: converting logical positions to canvas
pixels, computing the `baseScale` from canvas dimensions, clamping pan/zoom, and
hit-testing a position against a canvas point. This keeps the composable free of math.

**Expected Outcomes**
- A new file `MapCoordinates.kt` in `navigation/`.
- `fun computeBaseScale(canvasWidth: Float, canvasHeight: Float): Float` — fits the full
  terminal bounds (x: 60–620, y: 120–480) into the canvas at 90% fill.
- `fun MapTransform.toCanvasX(x: Float, baseScale: Float): Float` and `toCanvasY`.
- `fun MapTransform.clampPan(canvasWidth: Float, canvasHeight: Float, baseScale: Float): MapTransform` — prevents panning past the terminal edges.
- `fun MapTransform.applyPinch(centroid: Offset, zoomDelta: Float, panDelta: Offset, canvasSize: Size): MapTransform` — zoom-about-centroid.
- Zoom clamped to `[0.5f, 4.0f]`.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/navigation/MapCoordinates.kt`.
2. Define constants `MAP_LOGICAL_LEFT = 60f`, `MAP_LOGICAL_TOP = 120f`, `MAP_LOGICAL_RIGHT = 620f`, `MAP_LOGICAL_BOTTOM = 480f`.
3. Implement `computeBaseScale` using `minOf(canvasWidth / (RIGHT-LEFT), canvasHeight / (BOTTOM-TOP)) * 0.9f`.
4. Implement `toCanvasX(x, baseScale) = (x - MAP_LOGICAL_LEFT + panX) * baseScale * zoom`.
5. Implement `applyPinch` — translate centroid to logical space, apply zoom delta about that point, re-translate.
6. Implement `clampPan` using `MAP_LOGICAL_*` bounds.

**Relevant Context**
- `MapTransform` from Sub-Task 1.
- Canvas coordinate conversion currently in `RouteMapCard` at [`NavigationScreen.kt:553`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:553) — this replaces that logic.

**Status** — `[ ] pending`

---

### Sub-Task 3 — ViewModel Updates for Map State

**Intent**
Add map-interaction methods to `AirportNavigationViewModel` so the composable stays
stateless with respect to pan/zoom. Also expose `visibleFloor` which switches automatically
when navigation moves between floors, and can be overridden by the user.

**Expected Outcomes**
- `updateMapTransform(transform: MapTransform)` — called by the composable on gesture events.
- `resetMapView()` — return to default zoom and pan.
- `setVisibleFloor(floor: Int)` — user taps a floor button; updates `visibleFloor` in state.
- When `setCurrentLocation(position)` is called, if `position.floor != visibleFloor` and
  navigation is active, automatically call `setVisibleFloor(position.floor)`.
- `loadFloorPlan(floor: Int): FloorPlan` — delegates to `MockFloorPlanData`; cached in ViewModel.

**Todo List**
1. Add `private val floorPlanCache = mutableMapOf<Int, FloorPlan>()` to the ViewModel.
2. Add `fun loadFloorPlan(floor: Int): FloorPlan` — returns cached or calls `MockFloorPlanData.getFloorPlan(floor)`.
3. Add `fun updateMapTransform(transform: MapTransform)` — updates state.
4. Add `fun resetMapView()` — sets `mapTransform = MapTransform()`.
5. Add `fun setVisibleFloor(floor: Int)` — sets `visibleFloor = floor`, also calls `loadFloorPlan(floor)` to pre-warm cache.
6. In `setCurrentLocation`, after updating state, if `position.floor != navigationState.value.visibleFloor` and `voiceEngine != null`, call `setVisibleFloor(position.floor)`.

**Relevant Context**
- [`AirportNavigationViewModel.kt:137`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:137) — `setCurrentLocation`
- `AirportNavigationState` — adding `mapTransform` and `visibleFloor` (Sub-Task 1)

**Status** — `[ ] pending`

---

### Sub-Task 4 — Procedural Floor Plan Renderer

**Intent**
Create `IndoorMapCanvas.kt` — a composable that draws the airport floor plan
procedurally: corridor lines (filled rectangles representing walkable paths),
room/gate outlines, zone fills, and text labels. This is the base layer underneath
route and marker overlays.

**Expected Outcomes**
- A `@Composable fun IndoorMapCanvas(floorPlan: FloorPlan, transform: MapTransform, canvasSize: Size, modifier: Modifier)`.
- Corridors drawn as filled light-grey rectangles (width proportional to `Corridor.widthMeters`).
- Room outlines drawn with type-specific colors (gates=light-blue, lounges=purple-tint, food=orange-tint, security=red-tint, restrooms=teal-tint).
- Room labels drawn with small text, only visible when zoom ≥ 1.2.
- A thin outer border representing the terminal footprint.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/IndoorMapCanvas.kt`.
2. Implement `@Composable fun IndoorMapCanvas(...)` backed by a single `Canvas` block.
3. In the Canvas draw block:
   a. Draw terminal background: filled rect covering `MAP_LOGICAL_*` bounds, color `Color(0xFFF5F5F5)`.
   b. Draw corridors: for each `Corridor`, compute canvas rect from `start` and `end` + `widthMeters/2` perpendicular offset; fill with `Color(0xFFE0E0E0)`.
   c. Draw room outlines: for each `RoomOutline`, fill with type color (10% alpha), stroke border with type color (full alpha, 2dp).
   d. Draw room labels: `drawContext.canvas.nativeCanvas.drawText(...)` or use `drawIntoCanvas` for text; only when `transform.zoom >= 1.2f`.
4. Expose `val roomTypeColors: Map<RoomType, Color>` as a companion/top-level val for consistency with marker colors in Sub-Task 5.

**Relevant Context**
- `FloorPlan`, `Corridor`, `RoomOutline` from Sub-Task 1.
- `MapTransform.toCanvasX/Y` from Sub-Task 2.
- Existing Canvas drawing in [`NavigationScreen.kt:561`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:561) for reference style.

**Status** — `[ ] pending`

---

### Sub-Task 5 — User Location Marker with Animation

**Intent**
Create `UserLocationMarker.kt` — a composable that draws the user's current position
as an animated blue dot with a direction arrow, smoothly interpolating between
position updates using `Animatable`.

**Expected Outcomes**
- `@Composable fun UserLocationMarker(position: Position, heading: Float, transform: MapTransform, baseScale: Float)` drawn inside a Canvas block.
- Position animates smoothly: when `position` changes, `Animatable<Offset>` tweens from the old canvas offset to the new one over 800ms with `FastOutSlowInEasing`.
- Heading animates smoothly: `Animatable<Float>` rotates the arrow over 400ms.
- Marker appearance: outer pulsing ring (alpha 0.3, radius animated 20→28dp), solid blue circle (radius 14dp), white center dot (6dp), direction triangle arrow on top.
- The pulsing ring is driven by `rememberInfiniteTransition`.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/UserLocationMarker.kt`.
2. Declare `val animatedOffset = remember { Animatable(initialOffset, Offset.VectorConverter) }`.
3. `LaunchedEffect(position)` — `animatedOffset.animateTo(newOffset, tween(800, easing = FastOutSlowInEasing))`.
4. Declare `val animatedHeading = remember { Animatable(0f) }`.
5. `LaunchedEffect(heading)` — `animatedHeading.animateTo(heading, tween(400))`.
6. `val pulseAlpha by rememberInfiniteTransition().animateFloat(0.1f, 0.35f, infiniteRepeatable(tween(1200)))`.
7. In Canvas draw block: pulse ring → solid circle → center dot → rotate-canvas + draw triangle arrow.
8. Heading is computed in the ViewModel's `setCurrentLocation` from successive position deltas:
   `heading = atan2(newY - oldY, newX - oldX).toDegrees()`. Add `userHeading: Float = 0f` to `AirportNavigationState`.

**Relevant Context**
- `Animatable`, `rememberInfiniteTransition` — in existing Compose BOM.
- `MapTransform.toCanvasX/Y` from Sub-Task 2.
- `setCurrentLocation` in [`AirportNavigationViewModel.kt:137`](app/src/main/java/com/example/flightbooking/viewmodel/AirportNavigationViewModel.kt:137).

**Status** — `[ ] pending`

---

### Sub-Task 6 — Route Path Renderer

**Intent**
Create `RoutePathRenderer.kt` — a composable that draws the navigation route as a
colored polyline distinguishing completed (grey-green) from remaining (blue) path
segments, with animated direction arrows along the remaining path.

**Expected Outcomes**
- `@Composable fun RoutePathRenderer(route: NavigationRoute, currentStepIndex: Int, currentLocation: Position?, transform: MapTransform, baseScale: Float)`.
- Completed segments (instructions 0..<currentStepIndex) drawn as solid grey-green line (width 6dp).
- Remaining segments (currentStepIndex..<last) drawn as solid blue line (width 6dp) with animated dashes.
- Destination circle: red filled circle (radius 18dp) at `route.end`.
- Directional arrows (chevrons) every ~60dp along the remaining path, rotated to path bearing.
- Next waypoint highlighted: blinking blue circle (radius 10dp) at `route.instructions[currentStepIndex].position`.
- "Animated dashes" are drawn using `PathEffect.dashPathEffect` with the offset animated via `rememberInfiniteTransition` to give a flowing appearance.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/RoutePathRenderer.kt`.
2. From `route.waypoints`, build the full polyline. Split into completed vs. remaining segments based on `currentStepIndex`.
3. Draw completed path: `drawPath(completedPath, Color(0xFF78909C), Stroke(width = 6dp.toPx()))`.
4. Animate dash offset: `val dashOffset by rememberInfiniteTransition().animateFloat(0f, 40f, infiniteRepeatable(tween(800, easing = LinearEasing)))`.
5. Draw remaining path with `PathEffect.dashPathEffect(floatArrayOf(20f, 8f), dashOffset)`.
6. Draw chevron arrows along remaining path at equal intervals.
7. Draw destination marker and next-step blink marker.

**Relevant Context**
- `NavigationRoute.waypoints` in [`AirportModels.kt:134`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt:134).
- `currentStepIndex` from `AirportNavigationState`.
- Existing dashed path drawing at [`NavigationScreen.kt:588`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:588).

**Status** — `[ ] pending`

---

### Sub-Task 7 — POI Markers Overlay

**Intent**
Create `PoiMarkersOverlay.kt` — a composable that renders gate labels and amenity
icons on the map as small tappable markers. Labels only appear above a zoom threshold
to avoid clutter. Tapping a marker calls back into the ViewModel.

**Expected Outcomes**
- `@Composable fun PoiMarkersOverlay(gates: List<Gate>, amenities: List<Amenity>, visibleFloor: Int, transform: MapTransform, baseScale: Float, onGateTapped: (Gate) -> Unit, onAmenityTapped: (Amenity) -> Unit)`.
- Gate markers: small colored circle (status color) + gate number text visible at zoom ≥ 1.0.
- Amenity markers: colored dot by type, amenity type icon or first-letter label visible at zoom ≥ 1.5.
- `Modifier.pointerInput` hit-testing: on tap, find nearest marker within 24dp and invoke callback.
- Gate status colors: BOARDING=green, DELAYED=orange, AVAILABLE=grey, DEPARTED=dark-grey, CANCELLED=red, MAINTENANCE=yellow.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/PoiMarkersOverlay.kt`.
2. Implement `@Composable fun PoiMarkersOverlay(...)`.
3. Filter gates and amenities by `position.floor == visibleFloor`.
4. Use `Canvas` for drawing; add `Modifier.pointerInput(Unit) { detectTapGestures { offset -> /* hit test */ } }`.
5. Draw gate circles and labels using `drawContext.canvas.nativeCanvas` for text rendering.
6. Draw amenity dots with type colors from `roomTypeColors` map (Sub-Task 4).
7. Hit-test: on tap, iterate visible markers, find min Euclidean distance, if < 24dp trigger callback.

**Relevant Context**
- Gate/amenity positions from `AirportNavigationState`.
- `GateStatus` enum in [`AirportModels.kt:35`](app/src/main/java/com/example/flightbooking/data/models/AirportModels.kt:35).
- `roomTypeColors` from Sub-Task 4.

**Status** — `[ ] pending`

---

### Sub-Task 8 — Gesture Handler & Zoom/Pan Composable

**Intent**
Create `IndoorMapView.kt` — the main composable that stacks all layers (floor plan,
route, POIs, user marker) and wires up pinch-zoom and drag-pan gestures. This
replaces `RouteMapCard` entirely.

**Expected Outcomes**
- `@Composable fun IndoorMapView(state: AirportNavigationState, floorPlan: FloorPlan, onTransformChanged: (MapTransform) -> Unit, onGateTapped: (Gate) -> Unit, modifier: Modifier)`.
- `detectTransformGestures` handles pinch-zoom and two-finger pan.
- Single-finger drag also pans the map.
- All layers drawn in a single `Box` with `Modifier.clipToBounds()`.
- Layer order (bottom to top): `IndoorMapCanvas` → `RoutePathRenderer` → `PoiMarkersOverlay` → `UserLocationMarker`.
- A "re-center" FAB (floating action button) in the bottom-right corner snaps the map back to center on the user's position by calling `resetMapView()`.
- Canvas size is captured via `onSizeChanged` and stored in a `remember { mutableStateOf(Size.Zero) }` so coordinate math can use it.

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/IndoorMapView.kt`.
2. `val canvasSize = remember { mutableStateOf(Size.Zero) }`.
3. `val baseScale = remember(canvasSize.value) { computeBaseScale(canvasSize.value.width, canvasSize.value.height) }`.
4. `Box(modifier.onSizeChanged { canvasSize.value = it.toSize() })`.
5. Apply `Modifier.pointerInput(Unit) { detectTransformGestures { centroid, pan, zoom, _ -> onTransformChanged(transform.applyPinch(...)) } }`.
6. Apply `Modifier.pointerInput(Unit) { detectDragGestures { _, dragAmount -> onTransformChanged(transform.copy(panX = ..., panY = ...).clampPan(...)) } }`.
7. Stack composables in order.
8. Add `SmallFloatingActionButton` for re-center.

**Relevant Context**
- `detectTransformGestures` — `androidx.compose.foundation.gestures`.
- `MapTransform.applyPinch`, `clampPan` from Sub-Task 2.
- Sub-Tasks 4, 5, 6, 7 as child composables.

**Status** — `[ ] pending`

---

### Sub-Task 9 — Turn-by-Turn Navigation HUD

**Intent**
Replace the existing `CurrentStepCard` + `DistanceCard` arrangement with a compact
heads-up display (HUD) that floats over the map: a top-sliding card showing the next
instruction with distance, and a bottom bar showing total distance, walking time and
floor number.

**Expected Outcomes**
- `@Composable fun NavigationHud(state: AirportNavigationState, onMuteToggle: () -> Unit, modifier: Modifier)`.
- **Top card**: slides in from top with `AnimatedVisibility(slideInVertically)`. Shows: direction icon (larger, 48dp), instruction text, distance to next step. Background uses `MaterialTheme.colorScheme.primaryContainer`.
- **Bottom bar**: fixed at screen bottom. Shows: total distance remaining, walking time, current floor ("Floor 1"), mute button. Elevation shadow separates it from map.
- When `currentStepIndex` changes, the top card briefly scales up (pulse animation) to draw attention.
- Floor displayed as "Floor {visibleFloor}".

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/NavigationHud.kt`.
2. Implement `@Composable fun NavigationHud(...)`.
3. Top card: `AnimatedVisibility(navigationRoute != null, enter=slideInVertically+fadeIn, exit=slideOutVertically+fadeOut)`.
4. Add pulse on step change: `val scale by animateFloatAsState(if (justChanged) 1.08f else 1.0f)` with `LaunchedEffect(currentStepIndex)`.
5. Bottom bar: `Card` with `Row` containing distance, time, floor chip, mute icon.
6. Floor chip: small filled chip showing "Floor N", tapping it does nothing (floor switching is in Sub-Task 10).

**Relevant Context**
- Current `DistanceCard` at [`NavigationScreen.kt:237`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:237).
- Current `CurrentStepCard` at [`NavigationScreen.kt:295`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:295).
- `AnimatedVisibility`, `slideInVertically` — in existing Compose BOM.

**Status** — `[ ] pending`

---

### Sub-Task 10 — Floor Selector

**Intent**
Add a vertical floor selector panel on the right edge of the map. Tapping a floor
number loads the corresponding floor plan and updates `visibleFloor` in state.

**Expected Outcomes**
- `@Composable fun FloorSelector(floors: List<Int>, currentFloor: Int, userFloor: Int, onFloorSelected: (Int) -> Unit, modifier: Modifier)`.
- Rendered as a vertical `Column` of small circular buttons, positioned at the right edge of the map.
- Currently active floor highlighted (filled primary color).
- Floor where the user currently is shows a small blue dot indicator.
- Only shows floors that have gates or amenities (from terminal data).

**Todo List**
1. Create `app/src/main/java/com/example/flightbooking/ui/map/FloorSelector.kt`.
2. Compute `val availableFloors = terminal.gates.map { it.position.floor }.distinct().sorted()`.
3. Render each floor as a 40dp circle button.
4. Current floor: filled `MaterialTheme.colorScheme.primary`, white text.
5. User floor indicator: small blue dot below the matching floor button.
6. `onFloorSelected` calls `viewModel.setVisibleFloor(floor)`.

**Relevant Context**
- `visibleFloor` and `setVisibleFloor` from Sub-Task 3.
- Mock terminal has floors 1 and 2 (`MockData.getSampleGates()`).

**Status** — `[ ] pending`

---

### Sub-Task 11 — Rewrite NavigationScreen

**Intent**
Rewrite `NavigationScreen.kt` to use all the new composables: `IndoorMapView` as the
main body, `NavigationHud` floating over it, `FloorSelector` on the right edge, and
`DemoButton` / Previous / Next controls collapsed into a bottom sheet or small overlay.

**Expected Outcomes**
- `NavigationScreen` is a full-screen map with HUD overlaid — no `LazyColumn` scrolling.
- The top `TopAppBar` is retained (title, home button, mute button).
- The "All Steps" list is moved into a bottom sheet (`ModalBottomSheet`) toggled by a steps button in the top bar.
- The `DemoButton` is a small FAB in the bottom-left corner.
- The re-center FAB is provided by `IndoorMapView` (Sub-Task 8).
- Voice/TTS `DisposableEffect` is retained unchanged.
- The `NoNavigationState` fallback screen is retained unchanged.

**Todo List**
1. Rewrite the main body of `NavigationScreen` composable.
2. Replace `LazyColumn` with a `Box(Modifier.fillMaxSize())`.
3. Inside the Box: `IndoorMapView` fills the full area.
4. Overlay `NavigationHud` aligned to top + bottom of box.
5. Overlay `FloorSelector` aligned to `CenterEnd`.
6. Add a steps FAB (`ExtendedFloatingActionButton` with steps icon) at `BottomStart`; clicking it opens a `ModalBottomSheet` containing the existing `StepCard` list.
7. Add `DemoButton` FAB at `BottomStart` (below or beside steps FAB).
8. Load floor plan via `LaunchedEffect(visibleFloor) { viewModel.loadFloorPlan(visibleFloor) }`.
9. Forward `onTransformChanged` to `viewModel.updateMapTransform`.
10. Retain all `DisposableEffect`, `collectAsState`, voice wiring unchanged.

**Relevant Context**
- Current `NavigationScreen` at [`NavigationScreen.kt:42`](app/src/main/java/com/example/flightbooking/ui/screens/NavigationScreen.kt:42).
- `ModalBottomSheet` — available in Material3 (existing BOM).
- Sub-Tasks 8, 9, 10 as child composables.

**Status** — `[ ] pending`

---

## File Summary

| Action | File |
|--------|------|
| **Create** | `data/models/IndoorMapModels.kt` |
| **Create** | `data/MockFloorPlanData.kt` |
| **Create** | `navigation/MapCoordinates.kt` |
| **Create** | `ui/map/IndoorMapCanvas.kt` |
| **Create** | `ui/map/UserLocationMarker.kt` |
| **Create** | `ui/map/RoutePathRenderer.kt` |
| **Create** | `ui/map/PoiMarkersOverlay.kt` |
| **Create** | `ui/map/IndoorMapView.kt` |
| **Create** | `ui/map/NavigationHud.kt` |
| **Create** | `ui/map/FloorSelector.kt` |
| **Modify** | `data/models/AirportModels.kt` — add `mapTransform`, `visibleFloor`, `userHeading` to state |
| **Modify** | `viewmodel/AirportNavigationViewModel.kt` — add map methods, heading calc, floor auto-switch |
| **Rewrite** | `ui/screens/NavigationScreen.kt` — full-screen map with HUD overlay |

## No New Dependencies Required

All functionality uses APIs already available through the existing Compose BOM 2024.09.00:
- `Canvas`, `drawPath`, `drawCircle`, `drawRect`, `drawIntoCanvas` — existing
- `Animatable`, `rememberInfiniteTransition`, `animateFloatAsState` — existing
- `detectTransformGestures`, `detectDragGestures`, `detectTapGestures` — existing
- `AnimatedVisibility`, `slideInVertically`, `fadeIn` — existing
- `ModalBottomSheet` — existing Material3
- `SmallFloatingActionButton`, `ExtendedFloatingActionButton` — existing Material3
