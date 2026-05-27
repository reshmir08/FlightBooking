package com.example.flightbooking.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.data.models.FlightSortOption

/**
 * Flight Sort Bottom Sheet
 * Allows users to sort flights by different criteria
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightSortSheet(
    currentSort: FlightSortOption,
    onSortChange: (FlightSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sort options
            FlightSortOption.values().forEach { sortOption ->
                SortOptionItem(
                    sortOption = sortOption,
                    isSelected = currentSort == sortOption,
                    onClick = {
                        onSortChange(sortOption)
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * Sort Option Item Component
 */
@Composable
private fun SortOptionItem(
    sortOption: FlightSortOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getSortOptionTitle(sortOption),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = getSortOptionDescription(sortOption),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Get sort option title
 */
private fun getSortOptionTitle(sortOption: FlightSortOption): String {
    return when (sortOption) {
        FlightSortOption.PRICE_LOW_TO_HIGH -> "Price: Low to High"
        FlightSortOption.PRICE_HIGH_TO_LOW -> "Price: High to Low"
        FlightSortOption.DURATION_SHORTEST -> "Duration: Shortest"
        FlightSortOption.DURATION_LONGEST -> "Duration: Longest"
        FlightSortOption.DEPARTURE_EARLIEST -> "Departure: Earliest"
        FlightSortOption.DEPARTURE_LATEST -> "Departure: Latest"
        FlightSortOption.ARRIVAL_EARLIEST -> "Arrival: Earliest"
        FlightSortOption.ARRIVAL_LATEST -> "Arrival: Latest"
        FlightSortOption.BEST_VALUE -> "Best Value"
    }
}

/**
 * Get sort option description
 */
private fun getSortOptionDescription(sortOption: FlightSortOption): String {
    return when (sortOption) {
        FlightSortOption.PRICE_LOW_TO_HIGH -> "Find the cheapest flights first"
        FlightSortOption.PRICE_HIGH_TO_LOW -> "Show premium options first"
        FlightSortOption.DURATION_SHORTEST -> "Get there faster"
        FlightSortOption.DURATION_LONGEST -> "More time in the air"
        FlightSortOption.DEPARTURE_EARLIEST -> "Leave as early as possible"
        FlightSortOption.DEPARTURE_LATEST -> "Leave as late as possible"
        FlightSortOption.ARRIVAL_EARLIEST -> "Arrive as early as possible"
        FlightSortOption.ARRIVAL_LATEST -> "Arrive as late as possible"
        FlightSortOption.BEST_VALUE -> "Balance of price, duration, and convenience"
    }
}

// Made with Bob
