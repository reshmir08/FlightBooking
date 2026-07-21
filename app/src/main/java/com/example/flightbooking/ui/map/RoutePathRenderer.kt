package com.example.flightbooking.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.data.models.NavigationIcon
import com.example.flightbooking.data.models.NavigationRoute
import com.example.flightbooking.data.models.Position
import com.example.flightbooking.navigation.MapCoordinates
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * RoutePathRenderer
 *
 * Draws the navigation route as a coloured polyline on the indoor map:
 *
 * - **Completed segments** (steps before [currentStepIndex]): solid grey-green line
 * - **Remaining segments** (current step onward): blue animated-dash line
 * - **Directional chevrons** along the remaining path every ~60dp
 * - **Next step marker**: blinking blue circle at the current step waypoint
 * - **Destination marker**: solid red circle at [route.end]
 *
 * All coordinates are converted via [MapCoordinates] using [transform] and [baseScale].
 */
@Composable
fun RoutePathRenderer(
    route: NavigationRoute,
    currentStepIndex: Int,
    transform: MapTransform,
    baseScale: Float,
    modifier: Modifier = Modifier
) {
    // Animated dash offset for remaining path (gives a flowing "marching ants" effect)
    val infiniteTransition = rememberInfiniteTransition(label = "routeDash")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashOffset"
    )

    // Blinking alpha for next-step marker
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Canvas(modifier = modifier) {
        val waypoints = route.waypoints
        if (waypoints.size < 2) return@Canvas

        // Map all waypoints to canvas offsets
        val canvasPoints = waypoints.map { pos ->
            MapCoordinates.toCanvas(pos, baseScale, transform)
        }

        // Determine split point — which canvas point corresponds to the current step
        // The route has waypoints [from, midpoint, to]; step index is on instruction list.
        // We split the polyline at the midpoint (index 1) when currentStepIndex > 0.
        val splitIndex = when {
            currentStepIndex == 0                            -> 0
            currentStepIndex >= route.instructions.lastIndex -> canvasPoints.lastIndex
            else                                             -> 1.coerceAtMost(canvasPoints.lastIndex)
        }

        // ── Completed path ─────────────────────────────────────────────────────
        if (splitIndex > 0) {
            val completedPoints = canvasPoints.subList(0, splitIndex + 1)
            drawPolyline(
                points  = completedPoints,
                color   = Color(0xFF78909C),   // blue-grey (completed)
                width   = 5.dp.toPx(),
                dashed  = false,
                dashOff = 0f
            )
        }

        // ── Remaining path ─────────────────────────────────────────────────────
        if (splitIndex < canvasPoints.lastIndex) {
            val remainingPoints = canvasPoints.subList(splitIndex, canvasPoints.size)
            drawPolyline(
                points  = remainingPoints,
                color   = Color(0xFF2196F3),   // blue (remaining)
                width   = 5.dp.toPx(),
                dashed  = true,
                dashOff = dashOffset
            )

            // Directional chevrons
            drawChevrons(remainingPoints, Color(0xFF2196F3).copy(alpha = 0.7f))
        }

        // ── Next-step marker ───────────────────────────────────────────────────
        val nextStepPos = route.instructions.getOrNull(currentStepIndex)?.position
        if (nextStepPos != null) {
            val nextCanvas = MapCoordinates.toCanvas(nextStepPos, baseScale, transform)
            drawCircle(
                color  = Color(0xFF2196F3).copy(alpha = blinkAlpha),
                radius = 8.dp.toPx(),
                center = nextCanvas
            )
            drawCircle(
                color  = Color.White,
                radius = 4.dp.toPx(),
                center = nextCanvas
            )
        }

        // ── Destination marker ─────────────────────────────────────────────────
        val destCanvas = MapCoordinates.toCanvas(route.end, baseScale, transform)
        drawCircle(color = Color(0xFFF44336), radius = 12.dp.toPx(), center = destCanvas)
        drawCircle(color = Color.White,       radius = 5.dp.toPx(),  center = destCanvas)
        drawCircle(
            color  = Color(0xFFF44336),
            radius = 12.dp.toPx(),
            center = destCanvas,
            style  = Stroke(width = 2.dp.toPx())
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun DrawScope.drawPolyline(
    points: List<Offset>,
    color: Color,
    width: Float,
    dashed: Boolean,
    dashOff: Float
) {
    if (points.size < 2) return

    val pathEffect = if (dashed) {
        PathEffect.dashPathEffect(floatArrayOf(20f, 8f), phase = dashOff)
    } else null

    val stroke = Stroke(
        width      = width,
        cap        = StrokeCap.Round,
        join       = StrokeJoin.Round,
        pathEffect = pathEffect
    )

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path = path, color = color, style = stroke)
}

/**
 * Draw small arrowhead chevrons along [points] at approximately every 60dp.
 */
private fun DrawScope.drawChevrons(points: List<Offset>, color: Color) {
    val spacing = 60.dp.toPx()
    var accumulated = spacing / 2f  // start half-spacing in so first chevron is not at the very start

    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end   = points[i + 1]
        val segLen = distance(start, end)
        if (segLen < 1f) continue

        var remaining = segLen
        var cursor    = 0f

        while (cursor < segLen) {
            val t    = cursor / segLen
            val cx   = start.x + (end.x - start.x) * t
            val cy   = start.y + (end.y - start.y) * t
            val angleDeg = Math.toDegrees(atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())).toFloat()

            drawChevron(Offset(cx, cy), angleDeg, color)
            cursor += spacing
            if (cursor >= segLen) break
        }
    }
}

private fun DrawScope.drawChevron(center: Offset, angleDeg: Float, color: Color) {
    val size = 7.dp.toPx()
    rotate(degrees = angleDeg, pivot = center) {
        val path = Path().apply {
            moveTo(center.x - size,  center.y - size * 0.6f)
            lineTo(center.x,         center.y)
            lineTo(center.x - size,  center.y + size * 0.6f)
        }
        drawPath(
            path  = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    return sqrt(dx * dx + dy * dy)
}

// Made with Bob
