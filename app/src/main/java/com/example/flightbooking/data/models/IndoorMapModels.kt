package com.example.flightbooking.data.models

/**
 * Indoor Map Data Models
 *
 * All coordinates are expressed in the same logical coordinate space used by
 * [Position]: x ∈ [60, 620], y ∈ [120, 480]. No Android dependencies so these
 * are fully unit-testable.
 */

// ── Floor plan geometry ────────────────────────────────────────────────────────

/** A walkable corridor segment. Width is in logical units (≈ meters). */
data class Corridor(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val floor: Int,
    val widthUnits: Float = 20f          // half-width on each side of the centre line
)

/** Axis-aligned rectangle in logical coordinates. */
data class MapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
}

/** Categorises a room for color/icon selection. */
enum class RoomType {
    GATE, LOUNGE, CAFE, RESTROOM, FOOD_COURT, SECURITY, GENERAL, STAIRS_ELEVATOR
}

/** A labeled room or zone drawn as a filled rectangle on the floor plan. */
data class RoomOutline(
    val id: String,
    val label: String,
    val bounds: MapRect,
    val floor: Int,
    val type: RoomType
)

/** All geometry needed to render one floor of the terminal. */
data class FloorPlan(
    val floor: Int,
    val corridors: List<Corridor>,
    val rooms: List<RoomOutline>
)

// ── Map transform (pan + zoom) ─────────────────────────────────────────────────

/**
 * Current pan and zoom state for the indoor map.
 *
 * [panX] / [panY]: logical-unit offset applied before scaling. Positive values
 * shift the viewport left/up (i.e. the map appears to move right/down).
 *
 * [zoom]: multiplier on top of the base scale that fits the full terminal
 * into the canvas at zoom = 1.0.
 */
data class MapTransform(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

// Made with Bob
