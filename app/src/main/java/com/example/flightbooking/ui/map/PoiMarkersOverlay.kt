package com.example.flightbooking.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.Amenity
import com.example.flightbooking.data.models.AmenityType
import com.example.flightbooking.data.models.Gate
import com.example.flightbooking.data.models.GateStatus
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.navigation.MapCoordinates
import kotlin.math.sqrt

/**
 * PoiMarkersOverlay
 *
 * Draws gate and amenity markers on the indoor map for the [visibleFloor].
 *
 * - Gate markers are colored by boarding status; labels appear at zoom ≥ 1.0.
 * - Amenity markers use the [roomTypeColors] palette; labels appear at zoom ≥ 1.5.
 * - Tapping within 24dp of a marker fires the corresponding callback.
 */
@Composable
fun PoiMarkersOverlay(
    gates: List<Gate>,
    amenities: List<Amenity>,
    visibleFloor: Int,
    transform: MapTransform,
    baseScale: Float,
    onGateTapped: (Gate) -> Unit,
    onAmenityTapped: (Amenity) -> Unit,
    modifier: Modifier = Modifier
) {
    val floorGates     = gates.filter     { it.position.floor == visibleFloor }
    val floorAmenities = amenities.filter { it.position.floor == visibleFloor }

    // Pre-compute canvas positions for hit-testing
    val gateOffsets     = floorGates.map     { MapCoordinates.toCanvas(it.position,     baseScale, transform) }
    val amenityOffsets  = floorAmenities.map { MapCoordinates.toCanvas(it.position,     baseScale, transform) }

    val tapThresholdPx = 28.dp  // generous tap target

    Canvas(
        modifier = modifier.pointerInput(floorGates, floorAmenities, transform, baseScale) {
            detectTapGestures { tapOffset ->
                val threshPx = tapThresholdPx.toPx()
                // Check gates first
                val gateHit = gateOffsets.indexOfFirst { dist(it, tapOffset) <= threshPx }
                if (gateHit >= 0) { onGateTapped(floorGates[gateHit]); return@detectTapGestures }
                // Then amenities
                val amenityHit = amenityOffsets.indexOfFirst { dist(it, tapOffset) <= threshPx }
                if (amenityHit >= 0) onAmenityTapped(floorAmenities[amenityHit])
            }
        }
    ) {
        // ── Amenity markers ────────────────────────────────────────────────────
        floorAmenities.forEachIndexed { i, amenity ->
            val offset = amenityOffsets[i]
            drawAmenityMarker(amenity, offset, transform.zoom)
        }

        // ── Gate markers (drawn on top) ────────────────────────────────────────
        floorGates.forEachIndexed { i, gate ->
            val offset = gateOffsets[i]
            drawGateMarker(gate, offset, transform.zoom)
        }
    }
}

// ── Gate drawing ───────────────────────────────────────────────────────────────

private val gateStatusColors = mapOf(
    GateStatus.BOARDING     to Color(0xFF43A047),   // green
    GateStatus.AVAILABLE    to Color(0xFF90A4AE),   // blue-grey
    GateStatus.DELAYED      to Color(0xFFFFA726),   // orange
    GateStatus.DEPARTED     to Color(0xFF78909C),   // dark grey
    GateStatus.CANCELLED    to Color(0xFFEF5350),   // red
    GateStatus.MAINTENANCE  to Color(0xFFFFEE58)    // yellow
)

private fun DrawScope.drawGateMarker(gate: Gate, center: Offset, zoom: Float) {
    val radius = 7.dp.toPx()
    val color  = gateStatusColors[gate.status] ?: Color.Gray

    drawCircle(color = color,        radius = radius,          center = center)
    drawCircle(color = Color.White,  radius = radius * 0.45f,  center = center)
    drawCircle(
        color  = color.copy(alpha = 0.6f),
        radius = radius,
        center = center,
        style  = Stroke(width = 1.5f.dp.toPx())
    )

    if (zoom >= 1.0f) {
        drawGateLabel(gate.number, center, color, radius, zoom)
    }
}

private fun DrawScope.drawGateLabel(
    label: String,
    center: Offset,
    color: Color,
    markerRadius: Float,
    zoom: Float
) {
    val textSizePx = (9f * zoom).coerceIn(9f, 20f) * density
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color    = color.toArgb()
            textAlign     = android.graphics.Paint.Align.CENTER
            textSize      = textSizePx
            isAntiAlias   = true
            isFakeBoldText = true
        }
        canvas.nativeCanvas.drawText(label, center.x, center.y - markerRadius - 4f, paint)
    }
}

// ── Amenity drawing ────────────────────────────────────────────────────────────

private val amenityTypeToRoomType = mapOf(
    AmenityType.LOUNGE          to com.example.flightbooking.data.models.RoomType.LOUNGE,
    AmenityType.CAFE            to com.example.flightbooking.data.models.RoomType.CAFE,
    AmenityType.COFFEE_SHOP     to com.example.flightbooking.data.models.RoomType.CAFE,
    AmenityType.RESTAURANT      to com.example.flightbooking.data.models.RoomType.FOOD_COURT,
    AmenityType.FOOD_COURT      to com.example.flightbooking.data.models.RoomType.FOOD_COURT,
    AmenityType.RESTROOM        to com.example.flightbooking.data.models.RoomType.RESTROOM,
    AmenityType.SECURITY        to com.example.flightbooking.data.models.RoomType.SECURITY,
    AmenityType.ELEVATOR        to com.example.flightbooking.data.models.RoomType.STAIRS_ELEVATOR,
    AmenityType.ESCALATOR       to com.example.flightbooking.data.models.RoomType.STAIRS_ELEVATOR,
    AmenityType.STAIRS          to com.example.flightbooking.data.models.RoomType.STAIRS_ELEVATOR,
)

private fun DrawScope.drawAmenityMarker(amenity: Amenity, center: Offset, zoom: Float) {
    val radius = 5.dp.toPx()
    val roomType = amenityTypeToRoomType[amenity.type]
    val color = if (roomType != null) roomTypeColors[roomType] ?: Color.Gray else Color(0xFFBDBDBD)

    drawCircle(color = color.copy(alpha = 0.85f), radius = radius, center = center)

    if (zoom >= 1.5f) {
        val initial = amenity.name.firstOrNull()?.toString() ?: "?"
        val textSizePx = (8f * zoom).coerceIn(8f, 16f) * density
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.color  = Color.White.toArgb()
                textAlign   = android.graphics.Paint.Align.CENTER
                textSize    = textSizePx
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawText(initial, center.x, center.y + textSizePx * 0.35f, paint)
        }
    }
}

// ── Utility ────────────────────────────────────────────────────────────────────

private fun dist(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    return sqrt(dx * dx + dy * dy)
}

// Made with Bob
