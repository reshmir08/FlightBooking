package com.example.flightbooking.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.R
import com.example.flightbooking.data.models.Flight
import com.example.flightbooking.ui.components.FlightCard
import com.example.flightbooking.ui.components.FlightFiltersSheet
import com.example.flightbooking.ui.components.FlightSortSheet
import com.example.flightbooking.viewmodel.FlightSearchViewModel

/**
 * Flight Results Screen
 * Displays search results with filtering, sorting, and comparison features
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightResultsScreen(
    viewModel: FlightSearchViewModel = viewModel(),
    onFlightClick: (Flight) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val searchResults by viewModel.searchResults.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Flight Results",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${searchResults.flights.size} flights found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Filters"
                        )
                    }
                    // Sort button
                    IconButton(onClick = { showSort = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_swap),
                            contentDescription = "Sort"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                searchResults.isLoading -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                searchResults.error != null -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = searchResults.error ?: "An error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { /* Retry logic */ }) {
                            Text("Retry")
                        }
                    }
                }
                searchResults.flights.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flight),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No flights found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Try adjusting your filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Comparison banner
                        if (searchResults.selectedFlightIds.isNotEmpty()) {
                            item {
                                ComparisonBanner(
                                    selectedCount = searchResults.selectedFlightIds.size,
                                    onCompareClick = { /* Navigate to comparison screen */ },
                                    onClearClick = { viewModel.clearSelections() }
                                )
                            }
                        }

                        // Flight cards
                        items(
                            items = searchResults.flights,
                            key = { it.id }
                        ) { flight ->
                            FlightCard(
                                flight = flight,
                                isSelected = searchResults.selectedFlightIds.contains(flight.id),
                                onFlightClick = { onFlightClick(flight) },
                                onCompareClick = { viewModel.toggleFlightSelection(flight.id) },
                                canCompare = searchResults.selectedFlightIds.size < 3 ||
                                        searchResults.selectedFlightIds.contains(flight.id)
                            )
                        }
                    }
                }
            }
        }

        // Filter bottom sheet
        if (showFilters) {
            FlightFiltersSheet(
                filters = searchResults.filters,
                onFiltersChange = { viewModel.updateFilters(it) },
                onDismiss = { showFilters = false }
            )
        }

        // Sort bottom sheet
        if (showSort) {
            FlightSortSheet(
                currentSort = searchResults.sortOption,
                onSortChange = { viewModel.updateSortOption(it) },
                onDismiss = { showSort = false }
            )
        }
    }
}

/**
 * Comparison banner shown when flights are selected for comparison
 */
@Composable
fun ComparisonBanner(
    selectedCount: Int,
    onCompareClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$selectedCount flight${if (selectedCount > 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Compare prices and features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onClearClick) {
                    Text("Clear")
                }
                Button(
                    onClick = onCompareClick,
                    enabled = selectedCount >= 2
                ) {
                    Text("Compare")
                }
            }
        }
    }
}

// Made with Bob
