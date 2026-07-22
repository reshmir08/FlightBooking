package com.example.flightbooking.ui.map

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.flightbooking.R
import com.example.flightbooking.data.models.AirportNavigationState
import com.example.flightbooking.data.models.Amenity
import com.example.flightbooking.data.models.FloorPlan
import com.example.flightbooking.data.models.Gate
import com.example.flightbooking.data.models.MapTransform
import com.example.flightbooking.navigation.MapCoordinates
import com.example.flightbooking.navigation.MapCoordinates.applyDrag
import com.example.flightbooking.navigation.MapCoordinates.applyPinch
import com.example.flightbooking.navigation.MapCoordinates.centredOn
import com.example.flightbooking.navigation.MapCoordinates.clampPan

/**
 * IndoorMapView
 *
 * The main map composable that stacks all rendering layers in order and handles
 * pinch-zoom and single-finger pan gestures.
 *
 * Layer order (bottom → top):
 *   1. [IndoorMapCanvas]   — procedural floor plan background
 *   2. [RoutePathRenderer] — route polyline (if navigating)
 *   3. [PoiMarkersOverlay] — gate / amenity dots
 *   4. [UserLocationMarker]— animated user dot + arrow
 *
 * A re-centre FAB in the bottom-right corner snaps the viewport back to the
 * user's position at the current zoom.
 */
@Composable
fun IndoorMapView(
    state: AirportNavigationState,
    floorPlan: FloorPlan,
    onTransformChanged: (MapTransform) -> Unit,
    onRecentreRequested: () -> Unit,
    onGateTapped: (Gate) -> Unit,
    onAmenityTapped: (Amenity) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val transform  = state.mapTransform
    val baseScale  = remember(canvasSize) { MapCoordinates.computeBaseScale(canvasSize) }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { canvasSize = it.toSize() }
            // ── Pinch-to-zoom ─────────────────────────────────────────────────
            .pointerInput(transform, baseScale, canvasSize) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    val updated = transform
                        .applyPinch(centroid, zoom, pan, baseScale)
                        .clampPan(canvasSize, baseScale)
                    onTransformChanged(updated)
                }
            }
            // ── Single-finger drag ────────────────────────────────────────────
            .pointerInput(transform, baseScale, canvasSize) {
                detectDragGestures { _, dragAmount ->
                    val updated = transform
                        .applyDrag(dragAmount, baseScale)
                        .clampPan(canvasSize, baseScale)
                    onTransformChanged(updated)
                }
            }
    ) {
        // ── Layer 1: Floor plan background ────────────────────────────────────
        IndoorMapCanvas(
            floorPlan  = floorPlan,
            transform  = transform,
            canvasSize = canvasSize,
            modifier   = Modifier.fillMaxSize()
        )

        // ── Layer 2: Route path ───────────────────────────────────────────────
        val route = state.navigationRoute
        if (route != null) {
            RoutePathRenderer(
                route            = route,
                currentStepIndex = state.currentStepIndex,
                transform        = transform,
                baseScale        = baseScale,
                modifier         = Modifier.fillMaxSize()
            )
        }

        // ── Layer 3: POI markers ───────────────────────────────────────────────
        PoiMarkersOverlay(
            gates          = state.currentTerminal?.gates    ?: emptyList(),
            amenities      = state.allAmenities,
            visibleFloor   = state.visibleFloor,
            transform      = transform,
            baseScale      = baseScale,
            onGateTapped   = onGateTapped,
            onAmenityTapped = onAmenityTapped,
            modifier       = Modifier.fillMaxSize()
        )

        // ── Layer 4: User location marker ─────────────────────────────────────
        val userLocation = state.currentLocation
        if (userLocation != null && userLocation.floor == state.visibleFloor) {
            UserLocationMarker(
                position  = userLocation,
                heading   = state.userHeading,
                transform = transform,
                baseScale = baseScale,
                modifier  = Modifier.fillMaxSize()
            )
        }
    }
}

// Made with Bob
