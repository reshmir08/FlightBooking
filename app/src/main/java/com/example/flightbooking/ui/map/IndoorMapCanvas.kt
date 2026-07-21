package com.example.flightbooking.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.Corridor
import com.example.flightbooking.data.models.FloorPlan
import com.example.flightbooking.data.models.MapRect
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.data.models.RoomOutline
import com.example.flightbooking.data.models.RoomType
import com.example.flightbooking.navigation.MapCoordinates
import com.example.flightbooking.navigation.MapCoordinates.MAP_BOTTOM
import com.example.flightbooking.navigation.MapCoordinates.MAP_LEFT
import com.example.flightbooking.navigation.MapCoordinates.MAP_RIGHT
import com.example.flightbooking.navigation.MapCoordinates.MAP_TOP
import com.example.flightbooking.navigation.MapCoordinates.toCanvas
import com.example.flightbooking.navigation.MapCoordinates.toCanvasX
import com.example.flightbooking.navigation.MapCoordinates.toCanvasY
import com.example.flightbooking.data.models.Position

/**
 * IndoorMapCanvas
 *
 * Procedurally renders the airport terminal floor plan:
 * 1. Terminal boundary background
 * 2. Walkable corridor fills
 * 3. Room / zone outlines with type-specific colours
 * 4. Room labels (visible above zoom 1.2)
 */

// ── Room-type colour palette ───────────────────────────────────────────────────

val roomTypeColors: Map<RoomType, Color> = mapOf(
    RoomType.GATE            to Color(0xFF42A5F5),   // blue
    RoomType.LOUNGE          to Color(0xFFAB47BC),   // purple
    RoomType.CAFE            to Color(0xFFFF8A65),   // orange
    RoomType.RESTROOM        to Color(0xFF26A69A),   // teal
    RoomType.FOOD_COURT      to Color(0xFFFF7043),   // deep-orange
    RoomType.SECURITY        to Color(0xFFEF5350),   // red
    RoomType.STAIRS_ELEVATOR to Color(0xFF78909C),   // blue-grey
    RoomType.GENERAL         to Color(0xFFBDBDBD)    // grey
)

private val corridorFill   = Color(0xFFE8EAED)
private val terminalBg     = Color(0xFFF5F6FA)
private val terminalBorder = Color(0xFFCFD8DC)

@Composable
fun IndoorMapCanvas(
    floorPlan: FloorPlan,
    transform: MapTransform,
    canvasSize: Size,
    modifier: Modifier = Modifier
) {
    val baseScale = MapCoordinates.computeBaseScale(canvasSize)

    Canvas(modifier = modifier) {
        // ── 1. Terminal background ─────────────────────────────────────────────
        drawTerminalBackground(baseScale, transform)

        // ── 2. Corridors ───────────────────────────────────────────────────────
        floorPlan.corridors.forEach { corridor ->
            drawCorridor(corridor, baseScale, transform)
        }

        // ── 3. Room outlines ───────────────────────────────────────────────────
        floorPlan.rooms.forEach { room ->
            drawRoom(room, baseScale, transform)
        }

        // ── 4. Room labels ─────────────────────────────────────────────────────
        if (transform.zoom >= 1.2f) {
            floorPlan.rooms.forEach { room ->
                drawRoomLabel(room, baseScale, transform)
            }
        }
    }
}

// ── Drawing helpers ────────────────────────────────────────────────────────────

private fun DrawScope.drawTerminalBackground(
    baseScale: Float,
    transform: MapTransform
) {
    val l = toCanvasX(MAP_LEFT,   baseScale, transform)
    val t = toCanvasY(MAP_TOP,    baseScale, transform)
    val r = toCanvasX(MAP_RIGHT,  baseScale, transform)
    val b = toCanvasY(MAP_BOTTOM, baseScale, transform)
    val w = (r - l).coerceAtLeast(0f)
    val h = (b - t).coerceAtLeast(0f)

    drawRoundRect(
        color = terminalBg,
        topLeft = Offset(l, t),
        size = Size(w, h),
        cornerRadius = CornerRadius(8.dp.toPx())
    )
    drawRoundRect(
        color = terminalBorder,
        topLeft = Offset(l, t),
        size = Size(w, h),
        cornerRadius = CornerRadius(8.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawCorridor(
    corridor: Corridor,
    baseScale: Float,
    transform: MapTransform
) {
    val sx = toCanvasX(corridor.startX, baseScale, transform)
    val sy = toCanvasY(corridor.startY, baseScale, transform)
    val ex = toCanvasX(corridor.endX,   baseScale, transform)
    val ey = toCanvasY(corridor.endY,   baseScale, transform)

    val effectiveScale = baseScale * transform.zoom
    val halfW = corridor.widthUnits * effectiveScale / 2f

    // Determine if the corridor is horizontal or vertical and draw accordingly
    val isHorizontal = kotlin.math.abs(corridor.endY - corridor.startY) <
                       kotlin.math.abs(corridor.endX - corridor.startX)

    val (rectL, rectT, rectR, rectB) = if (isHorizontal) {
        listOf(
            minOf(sx, ex),
            minOf(sy, ey) - halfW,
            maxOf(sx, ex),
            maxOf(sy, ey) + halfW
        )
    } else {
        listOf(
            minOf(sx, ex) - halfW,
            minOf(sy, ey),
            maxOf(sx, ex) + halfW,
            maxOf(sy, ey)
        )
    }

    val rw = (rectR - rectL).coerceAtLeast(0f)
    val rh = (rectB - rectT).coerceAtLeast(0f)

    drawRect(
        color = corridorFill,
        topLeft = Offset(rectL, rectT),
        size = Size(rw, rh)
    )
}

private fun DrawScope.drawRoom(
    room: RoomOutline,
    baseScale: Float,
    transform: MapTransform
) {
    val color = roomTypeColors[room.type] ?: Color.LightGray
    val canvasRect = MapCoordinates.rectToCanvas(room.bounds, baseScale, transform)
    val w = (canvasRect.width).coerceAtLeast(0f)
    val h = (canvasRect.height).coerceAtLeast(0f)
    if (w < 1f || h < 1f) return

    drawRoundRect(
        color = color.copy(alpha = 0.18f),
        topLeft = Offset(canvasRect.left, canvasRect.top),
        size = Size(w, h),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
    drawRoundRect(
        color = color.copy(alpha = 0.80f),
        topLeft = Offset(canvasRect.left, canvasRect.top),
        size = Size(w, h),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = 1.5f.dp.toPx())
    )
}

private fun DrawScope.drawRoomLabel(
    room: RoomOutline,
    baseScale: Float,
    transform: MapTransform
) {
    val canvasRect = MapCoordinates.rectToCanvas(room.bounds, baseScale, transform)
    val w = canvasRect.width
    val h = canvasRect.height
    if (w < 10f || h < 6f) return

    val cx = canvasRect.left + w / 2f
    val cy = canvasRect.top  + h / 2f
    val color = roomTypeColors[room.type] ?: Color.DarkGray

    // Scale text size with zoom, clamped between 8 and 20 sp equivalent
    val textSizePx = (10f * transform.zoom).coerceIn(8f, 22f) * density

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.copy(alpha = 0.9f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = textSizePx
            isAntiAlias = true
            isFakeBoldText = room.type == RoomType.GATE
        }
        canvas.nativeCanvas.drawText(room.label, cx, cy + textSizePx / 3f, paint)
    }
}

// Made with Bob
