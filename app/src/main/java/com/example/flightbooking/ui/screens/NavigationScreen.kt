package com.example.flightbooking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import com.example.flightbooking.ui.map.IndoorMapView
import com.example.flightbooking.viewmodel.AirportNavigationViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

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
 *  │  DemoWalkFAB  (bottom-left)              │
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
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            // ── Row 2: Static compass rose ───────────────────────────────────
            StaticCompassRose(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            // ── Row 2b: Map + overlays ─────────────────────────────────────────
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 30.dp, start = 5.dp, end = 5.dp)
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

                // Demo Walk button — bottom-left corner of the map
                ExtendedFloatingActionButton(
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
                        MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (state.isDemoRunning)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (state.isDemoRunning) R.drawable.ic_stop
                                else R.drawable.ic_play_arrow
                            ),
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = if (state.isDemoRunning) "Stop Demo" else "Demo Walk",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
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
                // Walking time — use formattedTime which is kept live by setCurrentLocation
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = state.distanceToDestination?.formattedTime
                            ?.ifEmpty { "< 1 min" } ?: "< 1 min",
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

// ── Static Compass Rose ────────────────────────────────────────────────────────

/**
 * StaticCompassRose
 *
 * A fixed, non-rotating compass that always shows N at top, E right, S bottom, W left —
 * exactly matching the reference image: blue outer ring, N in red, E/S/W in dark grey,
 * 8 tick marks at 45° intervals.
 *
 * This is purely decorative/orientation aid. It does NOT rotate with user heading.
 */
@Composable
private fun StaticCompassRose(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = androidx.compose.ui.Modifier.size(size)) {
            val cx     = this.size.width  / 2f
            val cy     = this.size.height / 2f
            val radius = (this.size.minDimension / 2f) * 0.92f

            drawStaticCompass(cx, cy, radius)
        }
    }
}

private fun DrawScope.drawStaticCompass(cx: Float, cy: Float, radius: Float) {
    val ringColor = Color(0xFF3A5FA0)   // blue ring — matches reference
    val bgColor   = Color(0xFFF8F9FF)   // near-white fill inside circle
    val tickColor = Color(0xFF888888)   // subtle grey ticks
    val textDark  = Color(0xFF2B2B2B)   // E, S, W labels
    val textRed   = Color(0xFFCC2222)   // N label in red

    // ── Background circle ──────────────────────────────────────────────────────
    drawCircle(color = bgColor, radius = radius, center = Offset(cx, cy))

    // ── Outer blue ring ────────────────────────────────────────────────────────
    drawCircle(
        color  = ringColor,
        radius = radius,
        center = Offset(cx, cy),
        style  = Stroke(width = radius * 0.08f)
    )

    // ── 8 tick marks at 45° intervals ─────────────────────────────────────────
    val tickOuter = radius * 0.88f
    val tickInner = radius * 0.76f
    for (i in 0 until 8) {
        val angleRad = Math.toRadians((i * 45.0) - 90.0)   // -90° → index 0 = top = North
        val ox = cx + cos(angleRad).toFloat() * tickOuter
        val oy = cy + sin(angleRad).toFloat() * tickOuter
        val ix = cx + cos(angleRad).toFloat() * tickInner
        val iy = cy + sin(angleRad).toFloat() * tickInner
        drawLine(
            color       = tickColor,
            start       = Offset(ix, iy),
            end         = Offset(ox, oy),
            strokeWidth = radius * 0.045f
        )
    }

    // ── Cardinal labels: N (top/red), E (right), S (bottom), W (left) ─────────
    data class Dir(val label: String, val angleDeg: Double, val isNorth: Boolean = false)
    val dirs = listOf(
        Dir("N", -90.0, isNorth = true),
        Dir("E",   0.0),
        Dir("S",  90.0),
        Dir("W", 180.0)
    )

    val labelR      = radius * 0.54f
    val labelSizePx = radius * 0.40f

    drawIntoCanvas { canvas ->
        dirs.forEach { dir ->
            val rad  = Math.toRadians(dir.angleDeg)
            val tx   = cx + cos(rad).toFloat() * labelR
            val ty   = cy + sin(rad).toFloat() * labelR
            val paint = android.graphics.Paint().apply {
                color          = if (dir.isNorth) textRed.toArgb() else textDark.toArgb()
                textSize       = labelSizePx
                textAlign      = android.graphics.Paint.Align.CENTER
                isAntiAlias    = true
                isFakeBoldText = true
                typeface       = android.graphics.Typeface.DEFAULT_BOLD
            }
            // Shift down by ~35 % of text size so letter sits centred on its radial point
            canvas.nativeCanvas.drawText(dir.label, tx, ty + labelSizePx * 0.35f, paint)
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
