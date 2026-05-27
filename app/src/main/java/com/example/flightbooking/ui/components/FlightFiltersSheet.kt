package com.example.flightbooking.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.CabinClass
import com.example.flightbooking.data.models.FlightFilters

/**
 * Flight Filters Bottom Sheet
 * Allows users to filter flights by various criteria
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightFiltersSheet(
    filters: FlightFilters,
    onFiltersChange: (FlightFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var currentFilters by remember { mutableStateOf(filters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            currentFilters = FlightFilters()
                            onFiltersChange(currentFilters)
                        }
                    ) {
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            onFiltersChange(currentFilters)
                            onDismiss()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Price Range Filter
            FilterSection(title = "Price Range") {
                Column {
                    Text(
                        text = "$${currentFilters.minPrice.toInt()} - $${currentFilters.maxPrice.toInt()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RangeSlider(
                        value = currentFilters.minPrice.toFloat()..currentFilters.maxPrice.toFloat(),
                        onValueChange = { range ->
                            currentFilters = currentFilters.copy(
                                minPrice = range.start.toDouble(),
                                maxPrice = range.endInclusive.toDouble()
                            )
                        },
                        valueRange = 0f..5000f,
                        steps = 49
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$5000+",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Number of Stops Filter
            FilterSection(title = "Stops") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StopOption(
                        label = "Direct flights only",
                        selected = currentFilters.maxStops == 0,
                        onClick = {
                            currentFilters = currentFilters.copy(maxStops = 0)
                        }
                    )
                    StopOption(
                        label = "1 stop or less",
                        selected = currentFilters.maxStops == 1,
                        onClick = {
                            currentFilters = currentFilters.copy(maxStops = 1)
                        }
                    )
                    StopOption(
                        label = "2 stops or less",
                        selected = currentFilters.maxStops == 2,
                        onClick = {
                            currentFilters = currentFilters.copy(maxStops = 2)
                        }
                    )
                    StopOption(
                        label = "Any number of stops",
                        selected = currentFilters.maxStops == null,
                        onClick = {
                            currentFilters = currentFilters.copy(maxStops = null)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Departure Time Filter
            FilterSection(title = "Departure Time") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeRangeOption(
                        label = "Morning (6:00 - 11:59)",
                        selected = currentFilters.departureTimeRanges.contains("morning"),
                        onClick = {
                            currentFilters = currentFilters.copy(
                                departureTimeRanges = if (currentFilters.departureTimeRanges.contains("morning")) {
                                    currentFilters.departureTimeRanges - "morning"
                                } else {
                                    currentFilters.departureTimeRanges + "morning"
                                }
                            )
                        }
                    )
                    TimeRangeOption(
                        label = "Afternoon (12:00 - 17:59)",
                        selected = currentFilters.departureTimeRanges.contains("afternoon"),
                        onClick = {
                            currentFilters = currentFilters.copy(
                                departureTimeRanges = if (currentFilters.departureTimeRanges.contains("afternoon")) {
                                    currentFilters.departureTimeRanges - "afternoon"
                                } else {
                                    currentFilters.departureTimeRanges + "afternoon"
                                }
                            )
                        }
                    )
                    TimeRangeOption(
                        label = "Evening (18:00 - 23:59)",
                        selected = currentFilters.departureTimeRanges.contains("evening"),
                        onClick = {
                            currentFilters = currentFilters.copy(
                                departureTimeRanges = if (currentFilters.departureTimeRanges.contains("evening")) {
                                    currentFilters.departureTimeRanges - "evening"
                                } else {
                                    currentFilters.departureTimeRanges + "evening"
                                }
                            )
                        }
                    )
                    TimeRangeOption(
                        label = "Night (00:00 - 05:59)",
                        selected = currentFilters.departureTimeRanges.contains("night"),
                        onClick = {
                            currentFilters = currentFilters.copy(
                                departureTimeRanges = if (currentFilters.departureTimeRanges.contains("night")) {
                                    currentFilters.departureTimeRanges - "night"
                                } else {
                                    currentFilters.departureTimeRanges + "night"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duration Filter
            FilterSection(title = "Maximum Duration") {
                Column {
                    Text(
                        text = if (currentFilters.maxDuration != null) {
                            "${currentFilters.maxDuration!! / 60}h ${currentFilters.maxDuration!! % 60}m"
                        } else {
                            "Any duration"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = (currentFilters.maxDuration ?: 1440).toFloat(),
                        onValueChange = { value ->
                            currentFilters = currentFilters.copy(
                                maxDuration = if (value >= 1440f) null else value.toInt()
                            )
                        },
                        valueRange = 60f..1440f,
                        steps = 23
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "24h+",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cabin Class Filter
            FilterSection(title = "Cabin Class") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CabinClass.values().forEach { cabinClass ->
                        CabinClassOption(
                            cabinClass = cabinClass,
                            selected = currentFilters.cabinClasses.contains(cabinClass),
                            onClick = {
                                currentFilters = currentFilters.copy(
                                    cabinClasses = if (currentFilters.cabinClasses.contains(cabinClass)) {
                                        currentFilters.cabinClasses - cabinClass
                                    } else {
                                        currentFilters.cabinClasses + cabinClass
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Airlines Filter
            FilterSection(title = "Airlines") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sample airlines - in real app, this would be dynamic
                    listOf("Air Canada", "United Airlines", "Delta", "American Airlines", "WestJet").forEach { airline ->
                        AirlineOption(
                            airline = airline,
                            selected = currentFilters.airlines.contains(airline),
                            onClick = {
                                currentFilters = currentFilters.copy(
                                    airlines = if (currentFilters.airlines.contains(airline)) {
                                        currentFilters.airlines - airline
                                    } else {
                                        currentFilters.airlines + airline
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Section Component
 */
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

/**
 * Stop Option Component
 */
@Composable
private fun StopOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Time Range Option Component
 */
@Composable
private fun TimeRangeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Cabin Class Option Component
 */
@Composable
private fun CabinClassOption(
    cabinClass: CabinClass,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(cabinClass.name) },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Airline Option Component
 */
@Composable
private fun AirlineOption(
    airline: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = airline,
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() }
        )
    }
}

// Made with Bob
