package com.example.flightbooking.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.data.models.GateStatus
import com.example.flightbooking.R
import com.example.flightbooking.data.models.Amenity
import com.example.flightbooking.data.models.AmenityType
import com.example.flightbooking.data.models.Gate
import com.example.flightbooking.data.models.Position
import com.example.flightbooking.viewmodel.AirportNavigationViewModel

/**
 * Terminal Map Screen
 * Interactive map showing terminal layout, gates, and amenities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalMapScreen(
    viewModel: AirportNavigationViewModel = viewModel(),
    onAmenityClick: (Amenity) -> Unit = {},
    onGateClick: (Gate) -> Unit = {}
) {
    val navigationState by viewModel.navigationState.collectAsState()
    var selectedFilter by remember { mutableStateOf<AmenityType?>(null) }
    var showLegend by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal Map")
                        navigationState.currentTerminal?.let { terminal ->
                            Text(
                                text = "Terminal ${terminal.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // 2D/3D Toggle
                    IconButton(onClick = { viewModel.toggleMapView() }) {
                        Icon(
                            painter = painterResource(
                                id = if (navigationState.is3DView) R.drawable.ic_work else R.drawable.ic_flight
                            ),
                            contentDescription = if (navigationState.is3DView) "2D View" else "3D View"
                        )
                    }
                    
                    // Legend
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = "Legend"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In
                FloatingActionButton(
                    onClick = { viewModel.setMapZoom(navigationState.mapZoom + 0.2f) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
                
                // Zoom Out
                FloatingActionButton(
                    onClick = { viewModel.setMapZoom(navigationState.mapZoom - 0.2f) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("−", style = MaterialTheme.typography.headlineSmall)
                }
                
                // My Location
                FloatingActionButton(
                    onClick = {
                        // In real app, this would use device location
                        viewModel.setCurrentLocation(Position(50.0f, 50.0f, 1))
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "My Location"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                navigationState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                navigationState.error != null -> {
                    ErrorMessage(
                        error = navigationState.error!!,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Filter Chips
                        AmenityFilterRow(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { filter ->
                                selectedFilter = filter
                                viewModel.filterAmenitiesByType(filter)
                            }
                        )

                        // Map Canvas
                        InteractiveMap(
                            navigationState = navigationState,
                            selectedFilter = selectedFilter,
                            onAmenityClick = onAmenityClick,
                            onGateClick = onGateClick,
                            modifier = Modifier.weight(1f)
                        )

                        // Current Navigation Info
                        if (navigationState.destination != null) {
                            NavigationInfoBar(
                                destinationName = navigationState.destinationName ?: "",
                                distanceInfo = navigationState.distanceToDestination,
                                onClearNavigation = { viewModel.clearNavigation() }
                            )
                        }
                    }
                }
            }

            // Legend Overlay
            if (showLegend) {
                MapLegend(
                    onDismiss = { showLegend = false },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

/**
 * Amenity Filter Row
 */
@Composable
private fun AmenityFilterRow(
    selectedFilter: AmenityType?,
    onFilterSelected: (AmenityType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
            label = { Text("All") }
        )
        
        // Common amenity types
        listOf(
            AmenityType.RESTROOM,
            AmenityType.FOOD_COURT,
            AmenityType.COFFEE_SHOP,
            AmenityType.LOUNGE,
            AmenityType.CHARGING_STATION
        ).forEach { type ->
            FilterChip(
                selected = selectedFilter == type,
                onClick = { onFilterSelected(type) },
                label = { Text(type.name.replace("_", " ")) }
            )
        }
    }
}

/**
 * Interactive Map Component
 */
@Composable
private fun InteractiveMap(
    navigationState: com.example.flightbooking.data.models.AirportNavigationState,
    selectedFilter: AmenityType?,
    onAmenityClick: (Amenity) -> Unit,
    onGateClick: (Gate) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(navigationState.mapZoom) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                    offset += pan
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2 + offset.x
            val centerY = canvasHeight / 2 + offset.y

            // Draw terminal outline
            drawRect(
                color = Color.Gray,
                topLeft = Offset(centerX - 400 * scale, centerY - 300 * scale),
                size = androidx.compose.ui.geometry.Size(800 * scale, 600 * scale),
                style = Stroke(width = 3f)
            )

            // Draw gates
            navigationState.currentTerminal?.gates?.forEach { gate ->
                val gateX = centerX + (gate.position.x.toFloat() - 50) * scale * 8
                val gateY = centerY + (gate.position.y.toFloat() - 50) * scale * 8
                
                // Gate marker
                drawCircle(
                    color = when (gate.status) {
                        GateStatus.BOARDING -> Color.Green
                        GateStatus.DELAYED -> Color.Red
                        GateStatus.AVAILABLE -> Color.Blue
                        else -> Color.Gray
                    },
                    radius = 15f * scale,
                    center = Offset(gateX, gateY)
                )
            }

            // Draw amenities
            navigationState.filteredAmenities.forEach { amenity ->
                val amenityX = centerX + (amenity.position.x.toFloat() - 50) * scale * 8
                val amenityY = centerY + (amenity.position.y.toFloat() - 50) * scale * 8
                
                // Amenity marker
                drawCircle(
                    color = getAmenityColor(amenity.type),
                    radius = 10f * scale,
                    center = Offset(amenityX, amenityY)
                )
            }

            // Draw current location
            navigationState.currentLocation?.let { location ->
                val locX = centerX + (location.x.toFloat() - 50) * scale * 8
                val locY = centerY + (location.y.toFloat() - 50) * scale * 8
                
                // Pulsing circle for current location
                drawCircle(
                    color = Color.Blue,
                    radius = 20f * scale,
                    center = Offset(locX, locY),
                    alpha = 0.3f
                )
                drawCircle(
                    color = Color.Blue,
                    radius = 12f * scale,
                    center = Offset(locX, locY)
                )
            }

            // Draw navigation route
            navigationState.navigationRoute?.let { route ->
                val path = Path()
                route.waypoints.forEachIndexed { index, position ->
                    val x = centerX + (position.x.toFloat() - 50) * scale * 8
                    val y = centerY + (position.y.toFloat() - 50) * scale * 8
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 4f * scale)
                )
            }
        }

        // Map overlay info
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Zoom: ${String.format("%.1f", scale)}x",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Gates: ${navigationState.currentTerminal?.gates?.size ?: 0}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Amenities: ${navigationState.filteredAmenities.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Navigation Info Bar
 */
@Composable
private fun NavigationInfoBar(
    destinationName: String,
    distanceInfo: com.example.flightbooking.data.models.DistanceInfo?,
    onClearNavigation: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Navigating to",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = destinationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                distanceInfo?.let {
                    Text(
                        text = "${it.distance.toInt()}m • ${it.walkingTime} min walk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            IconButton(onClick = onClearNavigation) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Clear Navigation",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Map Legend
 */
@Composable
private fun MapLegend(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Legend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Close"
                    )
                }
            }

            Divider()

            LegendItem(color = Color.Blue, label = "Your Location")
            LegendItem(color = Color.Green, label = "Boarding")
            LegendItem(color = Color.Red, label = "Delayed")
            LegendItem(color = Color(0xFFFF9800), label = "Food & Drink")
            LegendItem(color = Color(0xFF9C27B0), label = "Restrooms")
            LegendItem(color = Color(0xFF4CAF50), label = "Lounges")
        }
    }
}

/**
 * Legend Item
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Error Message Component
 */
@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Helper function to get amenity color
 */
private fun getAmenityColor(type: AmenityType): Color {
    return when (type) {
        AmenityType.RESTROOM -> Color(0xFF9C27B0)
        AmenityType.FOOD_COURT, AmenityType.RESTAURANT, AmenityType.COFFEE_SHOP -> Color(0xFFFF9800)
        AmenityType.LOUNGE -> Color(0xFF4CAF50)
        AmenityType.CHARGING_STATION -> Color(0xFF2196F3)
        AmenityType.ATM, AmenityType.CURRENCY_EXCHANGE -> Color(0xFFFFC107)
        AmenityType.PHARMACY, AmenityType.MEDICAL_CENTER -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

// Made with Bob
