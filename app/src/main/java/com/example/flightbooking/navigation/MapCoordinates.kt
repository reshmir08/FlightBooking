package com.example.flightbooking.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.flightbooking.data.models.MapRect
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.data.models.Position
import kotlin.math.max

/**
 * MapCoordinates
 *
 * All coordinate math for the indoor map in one place.
 *
 * Logical coordinate space (same as [Position]):
 *   x: MAP_LEFT … MAP_RIGHT   (≈ 60 … 620)
 *   y: MAP_TOP  … MAP_BOTTOM  (≈ 120 … 480)
 *
 * Canvas pixel space: top-left = (0,0), grows right and down.
 *
 * Conversion:
 *   canvasX = (logicalX - MAP_LEFT  - transform.panX) * baseScale * transform.zoom
 *   canvasY = (logicalY - MAP_TOP   - transform.panY) * baseScale * transform.zoom
 */
object MapCoordinates {

    // ── Logical bounds of the terminal ────────────────────────────────────────

    const val MAP_LEFT   = 60f
    const val MAP_TOP    = 120f
    const val MAP_RIGHT  = 620f
    const val MAP_BOTTOM = 480f

    val MAP_WIDTH  get() = MAP_RIGHT - MAP_LEFT    // 560
    val MAP_HEIGHT get() = MAP_BOTTOM - MAP_TOP    // 360

    // ── Zoom clamp ─────────────────────────────────────────────────────────────

    const val ZOOM_MIN = 0.6f
    const val ZOOM_MAX = 4.0f

    // ── Base scale ────────────────────────────────────────────────────────────

    /**
     * Compute the scale factor that fits the entire terminal into [canvasSize]
     * at zoom = 1.0, leaving a 10 % margin on all sides.
     */
    fun computeBaseScale(canvasSize: Size): Float {
        if (canvasSize.width == 0f || canvasSize.height == 0f) return 1f
        val sx = canvasSize.width  / MAP_WIDTH
        val sy = canvasSize.height / MAP_HEIGHT
        return minOf(sx, sy) * 0.90f
    }

    // ── Logical → Canvas conversion ───────────────────────────────────────────

    fun toCanvasX(logicalX: Float, baseScale: Float, transform: MapTransform): Float =
        (logicalX - MAP_LEFT + transform.panX) * baseScale * transform.zoom

    fun toCanvasY(logicalY: Float, baseScale: Float, transform: MapTransform): Float =
        (logicalY - MAP_TOP  + transform.panY) * baseScale * transform.zoom

    fun toCanvas(pos: Position, baseScale: Float, transform: MapTransform): Offset =
        Offset(
            toCanvasX(pos.x, baseScale, transform),
            toCanvasY(pos.y, baseScale, transform)
        )

    // ── Canvas → Logical conversion ───────────────────────────────────────────

    fun toLogicalX(canvasX: Float, baseScale: Float, transform: MapTransform): Float {
        val effectiveScale = baseScale * transform.zoom
        return if (effectiveScale == 0f) 0f
        else canvasX / effectiveScale - transform.panX + MAP_LEFT
    }

    fun toLogicalY(canvasY: Float, baseScale: Float, transform: MapTransform): Float {
        val effectiveScale = baseScale * transform.zoom
        return if (effectiveScale == 0f) 0f
        else canvasY / effectiveScale - transform.panY + MAP_TOP
    }

    // ── MapRect → canvas rect corners ─────────────────────────────────────────

    fun rectToCanvas(
        rect: MapRect,
        baseScale: Float,
        transform: MapTransform
    ): androidx.compose.ui.geometry.Rect {
        val l = toCanvasX(rect.left,  baseScale, transform)
        val t = toCanvasY(rect.top,   baseScale, transform)
        val r = toCanvasX(rect.right, baseScale, transform)
        val b = toCanvasY(rect.bottom, baseScale, transform)
        return androidx.compose.ui.geometry.Rect(l, t, r, b)
    }

    // ── Gesture math ──────────────────────────────────────────────────────────

    /**
     * Apply a pinch-zoom gesture centred on [centroid] (canvas pixels).
     *
     * Zooms about the centroid so the logical point under the fingers stays fixed.
     * [zoomDelta] is the ratio from the gesture detector (e.g. 1.05 = 5 % larger).
     * [panDelta] is the two-finger translation in canvas pixels.
     */
    fun MapTransform.applyPinch(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        baseScale: Float
    ): MapTransform {
        val newZoom = (zoom * zoomDelta).coerceIn(ZOOM_MIN, ZOOM_MAX)
        val effectiveOld = baseScale * zoom
        val effectiveNew = baseScale * newZoom

        // Convert centroid to logical space under old transform, then re-derive panX/Y
        // so the logical point under the centroid stays at the same canvas pixel.
        val logicalCX = if (effectiveOld != 0f) centroid.x / effectiveOld - panX + MAP_LEFT else MAP_LEFT
        val logicalCY = if (effectiveOld != 0f) centroid.y / effectiveOld - panY + MAP_TOP  else MAP_TOP

        val newPanX = if (effectiveNew != 0f) centroid.x / effectiveNew - logicalCX + MAP_LEFT else panX
        val newPanY = if (effectiveNew != 0f) centroid.y / effectiveNew - logicalCY + MAP_TOP  else panY

        // Also apply the two-finger translation in logical units
        val logicalPanDX = if (effectiveNew != 0f) panDelta.x / effectiveNew else 0f
        val logicalPanDY = if (effectiveNew != 0f) panDelta.y / effectiveNew else 0f

        return copy(
            zoom = newZoom,
            panX = newPanX + logicalPanDX,
            panY = newPanY + logicalPanDY
        )
    }

    /**
     * Apply a single-finger drag ([dragDelta] in canvas pixels).
     */
    fun MapTransform.applyDrag(dragDelta: Offset, baseScale: Float): MapTransform {
        val effective = baseScale * zoom
        val dx = if (effective != 0f) dragDelta.x / effective else 0f
        val dy = if (effective != 0f) dragDelta.y / effective else 0f
        return copy(panX = panX + dx, panY = panY + dy)
    }

    /**
     * Clamp pan so the viewport cannot be dragged completely outside the terminal.
     * Allows 25 % overshoot on each side.
     */
    fun MapTransform.clampPan(canvasSize: Size, baseScale: Float): MapTransform {
        if (canvasSize == Size.Zero) return this
        val effective = baseScale * zoom
        if (effective == 0f) return this

        val visibleW = canvasSize.width  / effective
        val visibleH = canvasSize.height / effective
        val margin   = 0.25f  // allow 25 % of the map width/height as overshoot

        val minPanX = MAP_LEFT - MAP_WIDTH  * margin
        val maxPanX = MAP_RIGHT - visibleW  + MAP_WIDTH  * margin
        val minPanY = MAP_TOP   - MAP_HEIGHT * margin
        val maxPanY = MAP_BOTTOM - visibleH + MAP_HEIGHT * margin

        return copy(
            panX = -panX.coerceIn(
                -max(maxPanX, minPanX),
                -minPanX
            ).let { -it },   // re-express in existing sign convention
            panY = -panY.coerceIn(
                -max(maxPanY, minPanY),
                -minPanY
            ).let { -it }
        )
    }

    /**
     * Return a [MapTransform] that centres the map on [position] at the current zoom.
     */
    fun centredOn(position: Position, canvasSize: Size, baseScale: Float, zoom: Float = 1f): MapTransform {
        val effective = baseScale * zoom
        val targetCanvasX = canvasSize.width  / 2f
        val targetCanvasY = canvasSize.height / 2f
        val panX = if (effective != 0f) targetCanvasX / effective - position.x + MAP_LEFT else 0f
        val panY = if (effective != 0f) targetCanvasY / effective - position.y + MAP_TOP  else 0f
        return MapTransform(zoom = zoom, panX = panX, panY = panY)
    }
}

// Made with Bob
