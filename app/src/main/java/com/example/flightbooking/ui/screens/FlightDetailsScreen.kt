package com.example.flightbooking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * Flight Details Screen
 * Shows comprehensive flight information before booking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(
    flight: Flight,
    onBookClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Price",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatPrice(flight.price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = onBookClick,
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text("Continue to Book")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Airline Header
            AirlineHeader(flight = flight)

            Divider()

            // Flight Segments
            flight.segments.forEachIndexed { index, segment ->
                FlightSegmentDetails(
                    segment = segment,
                    segmentNumber = index + 1,
                    totalSegments = flight.segments.size
                )

                if (index < flight.segments.size - 1) {
                    LayoverInfo(
                        currentSegment = segment,
                        nextSegment = flight.segments[index + 1]
                    )
                }
            }

            Divider()

            // Baggage Information
            BaggageSection(flight = flight)

            Divider()

            // Amenities
            AmenitiesSection(flight = flight)

            Divider()

            // Fare Details
            FareDetailsSection(flight = flight)

            Divider()

            // Important Information
            ImportantInfoSection()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Airline Header Component
 */
@Composable
private fun AirlineHeader(flight: Flight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Airline logo placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = flight.airline.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flight.airline.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${flight.airline.code} ${flight.flightNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (flight.airline.rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("%.1f/5.0", flight.airline.rating),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = flight.cabinClass.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Flight Segment Details Component
 */
@Composable
private fun FlightSegmentDetails(
    segment: FlightSegment,
    segmentNumber: Int,
    totalSegments: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Segment header
        if (totalSegments > 1) {
            Text(
                text = "Segment $segmentNumber of $totalSegments",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Departure
        FlightTimeLocation(
            time = segment.departureTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            date = segment.departureTime.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
            airport = segment.origin.name,
            code = segment.origin.code,
            city = segment.origin.city,
            terminal = segment.departureTerminal,
            gate = segment.departureGate,
            isArrival = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Flight duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flight),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(segment.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Flight time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arrival
        FlightTimeLocation(
            time = segment.arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            date = segment.arrivalTime.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
            airport = segment.destination.name,
            code = segment.destination.code,
            city = segment.destination.city,
            terminal = segment.arrivalTerminal,
            gate = segment.arrivalGate,
            isArrival = true
        )
    }
}

/**
 * Flight Time and Location Component
 */
@Composable
private fun FlightTimeLocation(
    time: String,
    date: String,
    airport: String,
    code: String,
    city: String,
    terminal: String?,
    gate: String?,
    isArrival: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = time,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$code - $airport",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = city,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (terminal != null || gate != null) {
                Text(
                    text = buildString {
                        if (terminal != null) append("Terminal $terminal")
                        if (terminal != null && gate != null) append(" • ")
                        if (gate != null) append("Gate $gate")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Layover Information Component
 */
@Composable
private fun LayoverInfo(
    currentSegment: FlightSegment,
    nextSegment: FlightSegment
) {
    val layoverMinutes = java.time.Duration.between(
        currentSegment.arrivalTime,
        nextSegment.departureTime
    ).toMinutes().toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    text = "Layover in ${currentSegment.destination.city}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDuration(layoverMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Baggage Section Component
 */
@Composable
private fun BaggageSection(flight: Flight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Baggage",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        BaggageItem(
            icon = R.drawable.ic_work,
            title = "Carry-on",
            description = "${flight.baggageInfo.carryOnAllowed} piece (${flight.baggageInfo.carryOnWeight})"
        )

        Spacer(modifier = Modifier.height(8.dp))

        BaggageItem(
            icon = R.drawable.ic_work,
            title = "Checked baggage",
            description = "${flight.baggageInfo.checkedBagsIncluded} piece included (${flight.baggageInfo.checkedBagWeight})"
        )

        if (flight.baggageInfo.additionalBagFee != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Additional bags: ${formatPrice(flight.baggageInfo.additionalBagFee)} each",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Baggage Item Component
 */
@Composable
private fun BaggageItem(
    icon: Int,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Amenities Section Component
 */
@Composable
private fun AmenitiesSection(flight: Flight) {
    if (flight.amenities.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Amenities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            flight.amenities.chunked(2).forEach { rowAmenities ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowAmenities.forEach { amenity ->
                        AmenityChip(
                            amenity = amenity,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number
                    if (rowAmenities.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Amenity Chip Component
 */
@Composable
private fun AmenityChip(
    amenity: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = amenity,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Fare Details Section Component
 */
@Composable
private fun FareDetailsSection(flight: Flight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Fare Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FareDetailRow(
            label = "Base fare",
            amount = flight.price * 0.85
        )
        FareDetailRow(
            label = "Taxes and fees",
            amount = flight.price * 0.15
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        FareDetailRow(
            label = "Total",
            amount = flight.price,
            isTotal = true
        )
    }
}

/**
 * Fare Detail Row Component
 */
@Composable
private fun FareDetailRow(
    label: String,
    amount: Double,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatPrice(amount),
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Important Information Section Component
 */
@Composable
private fun ImportantInfoSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Important Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Check-in opens 24 hours before departure\n" +
                        "• Arrive at the airport at least 2 hours before departure\n" +
                        "• Valid ID required for all passengers\n" +
                        "• Baggage fees may apply for additional items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Helper function to format duration
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

// Made with Bob
