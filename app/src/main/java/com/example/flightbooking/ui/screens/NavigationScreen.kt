package com.example.flightbooking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.R
import com.example.flightbooking.data.models.AirportNavigationState
import com.example.flightbooking.data.models.FloorPlan
import com.example.flightbooking.ui.map.FloorSelector
import com.example.flightbooking.ui.map.IndoorMapView
import com.example.flightbooking.viewmodel.AirportNavigationViewModel
import kotlinx.coroutines.delay

/**
 * NavigationScreen
 *
 * Full-screen 2D indoor map navigation experience:
 *
 *  ┌──────────────────────────────────────────┐
 *  │  TopAppBar  (title, stop, steps, mute)   │
 *  ├──────────────────────────────────────────┤
 *  │  NavigationHud — top instruction card    │
 *  │                                          │
 *  │  IndoorMapView ─────────────────────────│
 *  │   ├─ IndoorMapCanvas  (floor plan)       │
 *  │   ├─ RoutePathRenderer                   │
 *  │   ├─ PoiMarkersOverlay                   │
 *  │   └─ UserLocationMarker                  │
 *  │                                          │
 *  │         FloorSelector (right edge)       │
 *  │  DemoFAB  (bottom-left)                  │
 *  │                         Re-centre FAB    │
 *  │  NavigationHud — bottom info bar         │
 *  └──────────────────────────────────────────┘
 *
 * The "All Steps" list is in a [ModalBottomSheet] opened from the top-bar steps button.
 * Voice/TTS lifecycle is managed by [DisposableEffect] — unchanged from previous version.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: AirportNavigationViewModel = viewModel(),
    onStopNavigation: () -> Unit = {}
) {
    val state   = viewModel.navigationState.collectAsState().value
    val context = LocalContext.current

    // ── TTS lifecycle ──────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        viewModel.initVoiceSpeaker(context)
        onDispose { viewModel.shutdownVoiceSpeaker() }
    }

    // ── Load floor plan when visible floor changes ─────────────────────────────
    var floorPlan by remember { mutableStateOf<FloorPlan>(viewModel.loadFloorPlan(1)) }
    LaunchedEffect(state.visibleFloor) {
        floorPlan = viewModel.loadFloorPlan(state.visibleFloor)
    }

    // ── Bottom sheet state ─────────────────────────────────────────────────────
    var showStepsSheet by remember { mutableStateOf(false) }

    // ── Available floors ───────────────────────────────────────────────────────
    val availableFloors = remember(state.currentTerminal) {
        state.currentTerminal?.gates
            ?.map { it.position.floor }
            ?.distinct()
            ?.sorted()
            ?: listOf(1)
    }

    // ── No-route fallback ──────────────────────────────────────────────────────
    if (state.navigationRoute == null) {
        NoNavigationState(
            onStartNavigation = onStopNavigation,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // ── Main layout ────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Navigation")
                        if (state.destinationName.isNotEmpty()) {
                            Text(
                                text  = "To ${state.destinationName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onStopNavigation) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = "Stop navigation"
                        )
                    }
                },
                actions = {
                    // Steps list toggle
                    IconButton(onClick = { showStepsSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = "Show all steps"
                        )
                    }
                    // Mute toggle — volume icons
                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            painter = painterResource(
                                if (state.isMuted) R.drawable.ic_volume_off
                                else R.drawable.ic_volume_up
                            ),
                            contentDescription = if (state.isMuted) "Unmute voice guidance"
                                                 else "Mute voice guidance",
                            tint = if (state.isMuted) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->

        // Layout: Column with three rows —
        //   1. Instruction card  (wraps its own height)
        //   2. Map               (fills all remaining space — weight(1f))
        //   3. Bottom info bar   (wraps its own height)
        // FABs and floor selector are overlaid on the map Box only.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Row 1: Top instruction card ───────────────────────────────────
            TopInstructionCard(
                state    = state,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Row 2: Map + overlays ─────────────────────────────────────────
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)          // takes all space between card and bottom bar
            ) {
                IndoorMapView(
                    state               = state,
                    floorPlan           = floorPlan,
                    onTransformChanged  = { viewModel.updateMapTransform(it) },
                    onRecentreRequested = { viewModel.resetMapView() },
                    onGateTapped        = { gate -> viewModel.navigateToGate(gate.number) },
                    onAmenityTapped     = { amenity -> viewModel.setDestination(amenity.position, amenity.name) },
                    modifier            = Modifier.fillMaxSize()
                )

                // Floor selector — right edge, vertically centred within the map
                FloorSelector(
                    floors          = availableFloors,
                    currentFloor    = state.visibleFloor,
                    userFloor       = state.currentLocation?.floor ?: 1,
                    onFloorSelected = { viewModel.setVisibleFloor(it) },
                    modifier        = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )

                // Demo FAB — bottom-left corner of the map
                SmallFloatingActionButton(
                    onClick = {
                        if (state.isDemoRunning) viewModel.stopDemo()
                        else viewModel.startDemo()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp),
                    containerColor = if (state.isDemoRunning)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (state.isDemoRunning)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isDemoRunning) R.drawable.ic_star else R.drawable.ic_flight
                        ),
                        contentDescription = if (state.isDemoRunning) "Stop demo" else "Walk demo"
                    )
                }
            }

            // ── Row 3: Bottom info bar ────────────────────────────────────────
            BottomInfoBar(
                state           = state,
                onMuteToggle    = { viewModel.toggleMute() },
                onPrevStep      = { viewModel.jumpToStep(state.currentStepIndex - 1) },
                onNextStep      = { viewModel.jumpToStep(state.currentStepIndex + 1) },
                onRecalculate   = { viewModel.recalculate() },
                modifier        = Modifier.fillMaxWidth()
            )
        }
    }

    // ── All-steps bottom sheet ─────────────────────────────────────────────────
    if (showStepsSheet) {
        ModalBottomSheet(onDismissRequest = { showStepsSheet = false }) {
            val route = state.navigationRoute!!
            Text(
                text = "All Steps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(route.instructions) { index, instruction ->
                    StepCard(
                        instruction   = instruction.instruction,
                        stepNumber    = index + 1,
                        isCurrentStep = index == state.currentStepIndex,
                        isCompleted   = index < state.currentStepIndex,
                        onClick       = {
                            viewModel.jumpToStep(index)
                            showStepsSheet = false
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Reusable sub-composables ───────────────────────────────────────────────────

@Composable
private fun NoNavigationState(
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flight),
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
            Button(onClick = onStartNavigation) { Text("Find Destination") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepCard(
    instruction: String,
    stepNumber: Int,
    isCurrentStep: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentStep -> MaterialTheme.colorScheme.primaryContainer
                isCompleted   -> MaterialTheme.colorScheme.surfaceVariant
                else          -> MaterialTheme.colorScheme.surface
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
            Surface(
                modifier = Modifier.size(36.dp),
                shape    = CircleShape,
                color    = if (isCurrentStep || isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentStep) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = instruction,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentStep) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ── HUD composables (inlined into NavigationScreen to avoid Spacer/fillMaxSize issues) ──

/**
 * Top instruction card — overlaid at the top of the map with a semi-transparent
 * background so the map is still visible below it.
 */
@Composable
private fun TopInstructionCard(
    state: AirportNavigationState,
    modifier: Modifier = Modifier
) {
    val route = state.navigationRoute ?: return
    val instruction = route.instructions.getOrNull(state.currentStepIndex)
        ?: route.instructions.lastOrNull()

    // Pulse on step change
    var pulseTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.currentStepIndex) { pulseTrigger++ }
    var scaled by remember { mutableStateOf(false) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger == 0) return@LaunchedEffect
        scaled = true
        delay(180)
        scaled = false
    }
    val scale by animateFloatAsState(
        targetValue   = if (scaled) 1.03f else 1.0f,
        animationSpec = tween(180),
        label         = "hudPulse"
    )

    AnimatedVisibility(
        visible = true,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .scale(scale),
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = hudIcon(instruction?.instruction ?: "")),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = instruction?.instruction ?: "Calculating…",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if ((instruction?.distance ?: 0f) > 0f) {
                        Text(
                            text  = "${instruction!!.distance.toInt()} m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text     = "${state.currentStepIndex + 1}/${route.instructions.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Bottom info bar — docked to the bottom of the map.
 *
 * Row 1 (info): Distance · ETA · Floor chip · Mute
 * Row 2 (controls): Prev Step · Recalculate (if needed) · Next Step
 */
@Composable
private fun BottomInfoBar(
    state: AirportNavigationState,
    onMuteToggle: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val route = state.navigationRoute

    Surface(
        modifier        = modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation  = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── Info row ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance to destination
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = state.distanceToDestination?.formattedDistance ?: "—",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Walking time — formatted as "< 1 min" or "X min walk"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val walkMins = state.distanceToDestination?.walkingTime ?: 0
                    Text(
                        text = if (walkMins < 1) "< 1 min" else "$walkMins min walk",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = "ETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Floor indicator chip
                AssistChip(
                    onClick = {},
                    label   = { Text("Floor ${state.visibleFloor}") },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                // Mute — volume icon
                IconButton(onClick = onMuteToggle) {
                    Icon(
                        painter = painterResource(
                            if (state.isMuted) R.drawable.ic_volume_off
                            else R.drawable.ic_volume_up
                        ),
                        contentDescription = if (state.isMuted) "Unmute" else "Mute",
                        tint = if (state.isMuted) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Step navigation row ───────────────────────────────────────────
            if (route != null) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previous step
                    OutlinedButton(
                        onClick  = onPrevStep,
                        enabled  = state.currentStepIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", style = MaterialTheme.typography.labelMedium)
                    }

                    // Recalculate — shown in error container to draw attention
                    OutlinedButton(
                        onClick = onRecalculate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalc", style = MaterialTheme.typography.labelMedium)
                    }

                    // Next step
                    OutlinedButton(
                        onClick  = onNextStep,
                        enabled  = state.currentStepIndex < route.instructions.lastIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_flight),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun hudIcon(instruction: String): Int = when {
    instruction.contains("arrived",  ignoreCase = true) -> R.drawable.ic_star
    instruction.contains("stairs",   ignoreCase = true) ||
    instruction.contains("elevator", ignoreCase = true) -> R.drawable.ic_work
    instruction.contains("right",    ignoreCase = true) -> R.drawable.ic_flight
    instruction.contains("left",     ignoreCase = true) -> R.drawable.ic_flight
    instruction.contains("straight", ignoreCase = true) ||
    instruction.contains("forward",  ignoreCase = true) ||
    instruction.contains("walk",     ignoreCase = true) -> R.drawable.ic_flight
    else                                                 -> R.drawable.ic_info
}

@Preview
@Composable
fun PreviewNavigation(){
    NavigationScreen(viewModel = viewModel() ) {  }
}

// Made with Bob
