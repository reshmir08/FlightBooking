package com.example.flightbooking.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.data.models.Position
import com.example.flightbooking.navigation.MapCoordinates

/**
 * UserLocationMarker
 *
 * Draws the user's current position on the indoor map with:
 * - A pulsing accuracy ring (infinite animation)
 * - A solid blue circle (current position dot)
 * - A direction triangle arrow that rotates to match [heading]
 *
 * Both position and heading animate smoothly between updates so the
 * marker glides rather than jumps when [position] changes.
 */
@Composable
fun UserLocationMarker(
    position: Position,
    heading: Float,
    transform: MapTransform,
    baseScale: Float,
    modifier: Modifier = Modifier
) {
    // ── Animate canvas-space position ──────────────────────────────────────────
    val targetOffset = MapCoordinates.toCanvas(position, baseScale, transform)

    var animX by remember { mutableFloatStateOf(targetOffset.x) }
    var animY by remember { mutableFloatStateOf(targetOffset.y) }

    val smoothX by animateFloatAsState(
        targetValue = targetOffset.x,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "markerX"
    )
    val smoothY by animateFloatAsState(
        targetValue = targetOffset.y,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "markerY"
    )

    // ── Animate heading ────────────────────────────────────────────────────────
    val smoothHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "markerHeading"
    )

    // ── Pulsing accuracy ring ─────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue  = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier) {
        val center = Offset(smoothX, smoothY)
        drawUserMarker(center, smoothHeading, pulseRadius, pulseAlpha)
    }
}

// ── Drawing ────────────────────────────────────────────────────────────────────

private fun DrawScope.drawUserMarker(
    center: Offset,
    heading: Float,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    val markerRadius = 14.dp.toPx()
    val dotRadius    = 5.dp.toPx()
    val arrowSize    = 10.dp.toPx()

    val blue  = Color(0xFF2196F3)
    val white = Color.White

    // 1. Pulsing accuracy ring
    drawCircle(
        color  = blue.copy(alpha = pulseAlpha),
        radius = pulseRadius.dp.toPx(),
        center = center
    )

    // 2. Outer glow ring
    drawCircle(color = blue.copy(alpha = 0.25f), radius = markerRadius + 4.dp.toPx(), center = center)

    // 3. Solid blue circle
    drawCircle(color = blue,  radius = markerRadius, center = center)

    // 4. White centre dot
    drawCircle(color = white, radius = dotRadius,    center = center)

    // 5. Direction arrow — draw a filled triangle rotated to [heading]
    //    The triangle tip points up (canvas north) at 0° rotation.
    //    heading = 0° → east (atan2 convention).
    //    To align: rotate the tip clockwise by +90° to reach east, then add heading.
    rotate(degrees = heading + 90f, pivot = center) {
        val path = Path().apply {
            // Triangle tip points upward before rotation
            moveTo(center.x,                         center.y - markerRadius - arrowSize)
            lineTo(center.x - arrowSize * 0.5f,      center.y - markerRadius + arrowSize * 0.2f)
            lineTo(center.x + arrowSize * 0.5f,      center.y - markerRadius + arrowSize * 0.2f)
            close()
        }
        drawPath(path = path, color = white)
    }
}

// Made with Bob
