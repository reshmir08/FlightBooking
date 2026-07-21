package com.example.flightbooking.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.data.models.AirportNavigationState
import kotlinx.coroutines.delay

/**
 * NavigationHud
 *
 * Heads-up display overlaid on the indoor map:
 *
 * - **Top card** (slides in/out): next instruction + distance to that step.
 * - **Bottom bar** (always visible when navigating): total distance remaining,
 *   walking time, current floor chip, and mute toggle.
 *
 * When [AirportNavigationState.currentStepIndex] changes, the top card briefly
 * scales up (pulse) to draw the user's attention.
 */
@Composable
fun NavigationHud(
    state: AirportNavigationState,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val route = state.navigationRoute
    val navigating = route != null

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Top instruction card ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = navigating,
            enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
            exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(300))
        ) {
            if (route != null) {
                val instruction = route.instructions
                    .getOrNull(state.currentStepIndex)
                    ?: route.instructions.lastOrNull()

                // Pulse on step change
                var pulseTrigger by remember { mutableStateOf(0) }
                LaunchedEffect(state.currentStepIndex) {
                    pulseTrigger++
                }
                var scaled by remember { mutableStateOf(false) }
                LaunchedEffect(pulseTrigger) {
                    if (pulseTrigger == 0) return@LaunchedEffect
                    scaled = true
                    delay(180)
                    scaled = false
                }
                val scale by animateFloatAsState(
                    targetValue = if (scaled) 1.05f else 1.0f,
                    animationSpec = tween(180),
                    label = "hudPulse"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .scale(scale),
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Direction icon
                        Icon(
                            painter = painterResource(
                                id = getHudIcon(instruction?.instruction ?: "")
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = instruction?.instruction ?: "Calculating…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if ((instruction?.distance ?: 0f) > 0f) {
                                Text(
                                    text = "${instruction!!.distance.toInt()} m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                        .copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Step badge
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${state.currentStepIndex + 1}/${route.instructions.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Push bottom bar to the bottom via weight in parent
        Spacer(modifier = Modifier.weight(1f))

        // ── Bottom info bar ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = navigating,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300))
        ) {
            Surface(
                modifier  = Modifier.fillMaxWidth(),
                color     = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation  = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distance remaining
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.distanceToDestination?.formattedDistance ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Walking time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.distanceToDestination?.walkingTime ?: 0} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Walk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Floor chip
                    AssistChip(
                        onClick = { /* floor switching handled by FloorSelector */ },
                        label   = { Text("Floor ${state.visibleFloor}") },
                        colors  = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    // Mute button
                    IconButton(onClick = onMuteToggle) {
                        Icon(
                            painter = painterResource(
                                id = if (state.isMuted) R.drawable.ic_info else R.drawable.ic_flight
                            ),
                            contentDescription = if (state.isMuted) "Unmute" else "Mute",
                            tint = if (state.isMuted) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getHudIcon(instruction: String): Int = when {
    instruction.contains("right",    ignoreCase = true) -> R.drawable.ic_flight
    instruction.contains("left",     ignoreCase = true) -> R.drawable.ic_flight
    instruction.contains("straight", ignoreCase = true) ||
    instruction.contains("forward",  ignoreCase = true) -> R.drawable.ic_flight
    instruction.contains("stairs",   ignoreCase = true) ||
    instruction.contains("elevator", ignoreCase = true) -> R.drawable.ic_work
    instruction.contains("arrived",  ignoreCase = true) -> R.drawable.ic_star
    else                                                 -> R.drawable.ic_info
}

// Made with Bob
