package com.example.flightbooking.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FloorSelector
 *
 * A compact vertical panel of circular floor buttons placed on the right edge of the map.
 *
 * - The [currentFloor] (visible floor) is highlighted with a filled primary color.
 * - The [userFloor] (where the user physically is) shows a small blue indicator dot
 *   beneath its button so the user always knows which floor they are on.
 */
@Composable
fun FloorSelector(
    floors: List<Int>,
    currentFloor: Int,
    userFloor: Int,
    onFloorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (floors.size <= 1) return

    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Display floors in descending order (highest floor on top, like a building)
        floors.sortedDescending().forEach { floor ->
            FloorButton(
                floor        = floor,
                isSelected   = floor == currentFloor,
                isUserFloor  = floor == userFloor,
                onClick      = { onFloorSelected(floor) }
            )
        }
    }
}

@Composable
private fun FloorButton(
    floor: Int,
    isSelected: Boolean,
    isUserFloor: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = floor.toString(),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isSelected) MaterialTheme.colorScheme.onPrimary
                             else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // User indicator dot
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (isUserFloor) Color(0xFF2196F3) else Color.Transparent
                )
        )
    }
}

// Made with Bob
