package com.example.flightbooking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.R
import com.example.flightbooking.viewmodel.AirportNavigationViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Navigation Screen
 * Turn-by-turn navigation with real-time distance updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: AirportNavigationViewModel = viewModel(),
    onStopNavigation: () -> Unit = {}
) {
    val navigationState by viewModel.navigationState.collectAsState()
    var currentStepIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Navigation")
                        navigationState.destinationName?.let { name ->
                            Text(
                                text = "To $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onStopNavigation) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Stop Navigation"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (navigationState.navigationRoute == null) {
            // No active navigation
            NoNavigationState(
                onStartNavigation = onStopNavigation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            val route = navigationState.navigationRoute!!
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Distance and Time Card
                item {
                    DistanceCard(
                        distanceInfo = navigationState.distanceToDestination,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Route Map Visualization
                item {
                    RouteMapCard(
                        currentLocation = navigationState.currentLocation,
                        destination = navigationState.destination,
                        destinationName = navigationState.destinationName ?: "",
                        allAmenities = navigationState.allAmenities,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Current Step Card (Large)
                item {
                    CurrentStepCard(
                        instruction = route.instructions.getOrNull(currentStepIndex)?.instruction ?: "Calculating...",
                        stepNumber = currentStepIndex + 1,
                        totalSteps = route.instructions.size,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Navigation Controls
                item {
                    NavigationControls(
                        currentStep = currentStepIndex,
                        totalSteps = route.instructions.size,
                        onPreviousStep = {
                            if (currentStepIndex > 0) currentStepIndex--
                        },
                        onNextStep = {
                            if (currentStepIndex < route.instructions.size - 1) currentStepIndex++
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                }

                // All Steps List Header
                item {
                    Text(
                        text = "All Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // All Steps
                itemsIndexed(route.instructions) { index, navInstruction ->
                    StepCard(
                        instruction = navInstruction.instruction,
                        stepNumber = index + 1,
                        isCurrentStep = index == currentStepIndex,
                        isCompleted = index < currentStepIndex,
                        onClick = { currentStepIndex = index },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * No Navigation State
 */
@Composable
private fun NoNavigationState(
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_flight),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Active Navigation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select a destination to start navigating",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onStartNavigation) {
                Text("Find Destination")
            }
        }
    }
}

/**
 * Distance Card
 */
@Composable
private fun DistanceCard(
    distanceInfo: com.example.flightbooking.data.models.DistanceInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${distanceInfo?.distance?.toInt() ?: 0}m",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )

            // Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${distanceInfo?.walkingTime ?: 0} min",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Walking Time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Current Step Card (Large Display)
 */
@Composable
private fun CurrentStepCard(
    instruction: String,
    stepNumber: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Step $stepNumber of $totalSteps",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    painter = painterResource(id = getInstructionIcon(instruction)),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Instruction text
            Text(
                text = instruction,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Navigation Controls
 */
@Composable
private fun NavigationControls(
    currentStep: Int,
    totalSteps: Int,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous Button
        OutlinedButton(
            onClick = onPreviousStep,
            modifier = Modifier.weight(1f),
            enabled = currentStep > 0,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Previous")
        }

        // Next Button
        Button(
            onClick = onNextStep,
            modifier = Modifier.weight(1f),
            enabled = currentStep < totalSteps - 1,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text("Next")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_flight),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Step Card (List Item)
 */
@Composable
private fun StepCard(
    instruction: String,
    stepNumber: Int,
    isCurrentStep: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number or checkmark
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrentStep -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentStep) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Instruction
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentStep) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Icon
            Icon(
                painter = painterResource(id = getInstructionIcon(instruction)),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    isCurrentStep -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Route Map Card - Visual representation of the path with compass and landmarks
 */
@Composable
private fun RouteMapCard(
    currentLocation: com.example.flightbooking.data.models.Position?,
    destination: com.example.flightbooking.data.models.Position?,
    destinationName: String,
    allAmenities: List<com.example.flightbooking.data.models.Amenity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (currentLocation != null && destination != null) {
                // Draw the route map
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Calculate scale to fit both points
                    val minX = minOf(currentLocation.x, destination.x) - 50f
                    val maxX = maxOf(currentLocation.x, destination.x) + 50f
                    val minY = minOf(currentLocation.y, destination.y) - 50f
                    val maxY = maxOf(currentLocation.y, destination.y) + 50f
                    
                    val scaleX = canvasWidth / (maxX - minX)
                    val scaleY = canvasHeight / (maxY - minY)
                    val scale = minOf(scaleX, scaleY) * 0.8f // 80% to add padding
                    
                    // Center the map
                    val offsetX = (canvasWidth - (maxX - minX) * scale) / 2
                    val offsetY = (canvasHeight - (maxY - minY) * scale) / 2
                    
                    // Convert position to canvas coordinates
                    fun toCanvasX(x: Float) = (x - minX) * scale + offsetX
                    fun toCanvasY(y: Float) = (y - minY) * scale + offsetY
                    
                    val startX = toCanvasX(currentLocation.x)
                    val startY = toCanvasY(currentLocation.y)
                    val endX = toCanvasX(destination.x)
                    val endY = toCanvasY(destination.y)
                    
                    // Draw path with dashed line
                    val path = Path().apply {
                        moveTo(startX, startY)
                        
                        // Create a curved path for more natural look
                        val midX = (startX + endX) / 2
                        val midY = (startY + endY) / 2
                        val controlX = midX + (endY - startY) * 0.2f
                        val controlY = midY - (endX - startX) * 0.2f
                        
                        quadraticBezierTo(controlX, controlY, endX, endY)
                    }
                    
                    // Draw the path with dashed effect
                    drawPath(
                        path = path,
                        color = Color(0xFF2196F3),
                        style = Stroke(
                            width = 8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                    
                    // Draw current location marker (blue circle)
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = 16f,
                        center = Offset(startX, startY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(startX, startY)
                    )
                    
                    // Draw destination marker (red pin)
                    drawCircle(
                        color = Color(0xFFF44336),
                        radius = 20f,
                        center = Offset(endX, endY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10f,
                        center = Offset(endX, endY)
                    )
                    
                    // Find nearby amenities along the route
                    val nearbyAmenities = allAmenities.filter { amenity ->
                        val amenityX = toCanvasX(amenity.position.x)
                        val amenityY = toCanvasY(amenity.position.y)
                        // Check if amenity is near the path (within 80 units)
                        val distToStart = kotlin.math.sqrt((amenityX - startX) * (amenityX - startX) + (amenityY - startY) * (amenityY - startY))
                        val distToEnd = kotlin.math.sqrt((amenityX - endX) * (amenityX - endX) + (amenityY - endY) * (amenityY - endY))
                        distToStart < 150f || distToEnd < 150f
                    }.take(3) // Show max 3 nearby places
                    
                    // Draw nearby amenity markers (small gray circles)
                    nearbyAmenities.forEach { amenity ->
                        val amenityX = toCanvasX(amenity.position.x)
                        val amenityY = toCanvasY(amenity.position.y)
                        drawCircle(
                            color = Color(0xFF9E9E9E),
                            radius = 12f,
                            center = Offset(amenityX, amenityY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(amenityX, amenityY)
                        )
                    }
                }
                
                // Compass Rose (top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "N",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336),
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "W",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = "E",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = "S",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.offset(y = 8.dp)
                        )
                    }
                }
                
                // Labels
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                        )
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF44336))
                        )
                        Text(
                            text = destinationName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Nearby landmarks
                    if (currentLocation != null && destination != null) {
                        val nearbyAmenities = allAmenities.filter { amenity ->
                            val dx = amenity.position.x - currentLocation.x
                            val dy = amenity.position.y - currentLocation.y
                            val distToStart = kotlin.math.sqrt(dx * dx + dy * dy)
                            val dx2 = amenity.position.x - destination.x
                            val dy2 = amenity.position.y - destination.y
                            val distToEnd = kotlin.math.sqrt(dx2 * dx2 + dy2 * dy2)
                            distToStart < 100f || distToEnd < 100f
                        }.take(3)
                        
                        if (nearbyAmenities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nearby:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            nearbyAmenities.forEach { amenity ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF9E9E9E))
                                    )
                                    Text(
                                        text = amenity.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Map label
                Text(
                    text = "Route Overview",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Helper function to get instruction icon based on text
 */
private fun getInstructionIcon(instruction: String): Int {
    return when {
        instruction.contains("right", ignoreCase = true) -> R.drawable.ic_flight
        instruction.contains("left", ignoreCase = true) -> R.drawable.ic_flight
        instruction.contains("forward", ignoreCase = true) || 
        instruction.contains("straight", ignoreCase = true) -> R.drawable.ic_flight
        instruction.contains("stairs", ignoreCase = true) || 
        instruction.contains("elevator", ignoreCase = true) -> R.drawable.ic_work
        instruction.contains("arrived", ignoreCase = true) -> R.drawable.ic_star
        else -> R.drawable.ic_info
    }
}

// Made with Bob
