package com.example.flightbooking.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.example.flightbooking.data.models.Flight
import com.example.flightbooking.data.models.FlightSegment
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Flight Card Component
 * Displays flight information in a card format with comparison checkbox
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FlightCard(
    flight: Flight,
    isSelected: Boolean = false,
    onFlightClick: () -> Unit,
    onCompareClick: () -> Unit,
    canCompare: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onFlightClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Airline and Compare checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Airline logo placeholder
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = flight.airline.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column {
                        Text(
                            text = flight.airline.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (flight.airline.rating != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_star),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format("%.1f", flight.airline.rating),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Compare checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Compare",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { if (canCompare) onCompareClick() },
                        enabled = canCompare
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flight segments
            flight.segments.forEachIndexed { index, segment ->
                FlightSegmentInfo(segment = segment)
                
                if (index < flight.segments.size - 1) {
                    // Layover info
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Layover: ${calculateLayover(flight.segments[index], flight.segments[index + 1])}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flight details chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration
                DetailChip(
                    icon = R.drawable.ic_calendar,
                    text = formatDuration(flight.totalDuration)
                )
                
                // Stops
                DetailChip(
                    icon = R.drawable.ic_flight,
                    text = when (flight.segments.size) {
                        1 -> "Direct"
                        2 -> "1 stop"
                        else -> "${flight.segments.size - 1} stops"
                    }
                )
                
                // Cabin class
                DetailChip(
                    icon = R.drawable.ic_seat,
                    text = flight.cabinClass.name
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amenities
            if (flight.amenities.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    flight.amenities.take(4).forEach { amenity ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = getAmenityIcon(amenity)),
                                contentDescription = amenity,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = amenity,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Divider()

            Spacer(modifier = Modifier.height(12.dp))

            // Price and action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatPrice(flight.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per person",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onFlightClick,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Select")
                }
            }
        }
    }
}

/**
 * Flight Segment Information
 * Shows departure and arrival details for a single segment
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun FlightSegmentInfo(segment: FlightSegment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Departure
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = segment.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = segment.origin.code,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = segment.origin.city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Flight path
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_flight),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDuration(segment.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Arrival
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = segment.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = segment.destination.code,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = segment.destination.city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Detail Chip Component
 * Small chip showing flight detail with icon
 */
@Composable
private fun DetailChip(
    icon: Int,
    text: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Helper function to format duration in minutes to readable format
 */
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        "${hours}h ${mins}m"
    } else {
        "${mins}m"
    }
}

/**
 * Helper function to format price
 */
private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(price)
}

/**
 * Helper function to calculate layover time
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun calculateLayover(segment1: FlightSegment, segment2: FlightSegment): String {
    val layoverMinutes = java.time.Duration.between(
        segment1.arrivalTime,
        segment2.departureTime
    ).toMinutes().toInt()
    return formatDuration(layoverMinutes)
}

/**
 * Helper function to get amenity icon
 */
private fun getAmenityIcon(amenity: String): Int {
    return when (amenity.lowercase()) {
        "wifi" -> R.drawable.ic_info
        "entertainment" -> R.drawable.ic_star
        "meal" -> R.drawable.ic_discount
        "power" -> R.drawable.ic_info
        else -> R.drawable.ic_info
    }
}

// Made with Bob
