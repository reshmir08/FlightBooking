package com.example.flightbooking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.R
import com.example.flightbooking.data.models.Amenity
import com.example.flightbooking.data.models.AmenityType
import com.example.flightbooking.viewmodel.AirportNavigationViewModel
import com.example.flightbooking.viewmodel.getDistanceInfo

/**
 * Amenities List Screen
 * Shows all available amenities in the terminal with search and filter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenitiesListScreen(
    viewModel: AirportNavigationViewModel = viewModel(),
    onAmenityClick: (Amenity) -> Unit = {},
    onNavigateClick: (Amenity) -> Unit = {}
) {
    val navigationState by viewModel.navigationState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var sortByDistance by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amenities") },
                actions = {
                    // Sort by distance
                    if (navigationState.currentLocation != null) {
                        IconButton(
                            onClick = {
                                sortByDistance = !sortByDistance
                                if (sortByDistance) {
                                    viewModel.sortAmenitiesByDistance()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (sortByDistance) R.drawable.ic_star else R.drawable.ic_swap
                                ),
                                contentDescription = "Sort by distance",
                                tint = if (sortByDistance) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    
                    // Filter
                    IconButton(onClick = { showFilterSheet = true }) {
                        Badge(
                            containerColor = if (navigationState.selectedAmenityType != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Filter"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.searchAmenities(it)
                },
                modifier = Modifier.padding(16.dp)
            )

            // Quick Actions
            QuickActionsRow(
                onFindNearest = { type ->
                    viewModel.findNearestAmenity(type)
                }
            )

            Divider()

            // Amenities List
            when {
                navigationState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                navigationState.filteredAmenities.isEmpty() -> {
                    EmptyState(
                        message = if (searchQuery.isNotEmpty()) {
                            "No amenities found for \"$searchQuery\""
                        } else {
                            "No amenities available"
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Group by category if not searching
                        if (searchQuery.isEmpty() && navigationState.selectedAmenityType == null) {
                            val grouped = navigationState.filteredAmenities.groupBy { it.type }
                            grouped.forEach { (type, amenities) ->
                                item {
                                    Text(
                                        text = type.name.replace("_", " "),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(amenities) { amenity ->
                                    AmenityCard(
                                        amenity = amenity,
                                        distanceInfo = navigationState.getDistanceInfo(amenity),
                                        onClick = { onAmenityClick(amenity) },
                                        onNavigateClick = { onNavigateClick(amenity) }
                                    )
                                }
                            }
                        } else {
                            items(navigationState.filteredAmenities) { amenity ->
                                AmenityCard(
                                    amenity = amenity,
                                    distanceInfo = navigationState.getDistanceInfo(amenity),
                                    onClick = { onAmenityClick(amenity) },
                                    onNavigateClick = { onNavigateClick(amenity) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            AmenityFilterSheet(
                selectedType = navigationState.selectedAmenityType,
                onTypeSelected = { type ->
                    viewModel.filterAmenitiesByType(type)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

/**
 * Search Bar Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search amenities...") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true
    )
}

/**
 * Quick Actions Row
 */
@Composable
private fun QuickActionsRow(
    onFindNearest: (AmenityType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionChip(
            icon = R.drawable.ic_person,
            label = "Restroom",
            onClick = { onFindNearest(AmenityType.RESTROOM) }
        )
        QuickActionChip(
            icon = R.drawable.ic_discount,
            label = "Food",
            onClick = { onFindNearest(AmenityType.FOOD_COURT) }
        )
        QuickActionChip(
            icon = R.drawable.ic_star,
            label = "Lounge",
            onClick = { onFindNearest(AmenityType.LOUNGE) }
        )
        QuickActionChip(
            icon = R.drawable.ic_info,
            label = "Charging",
            onClick = { onFindNearest(AmenityType.CHARGING_STATION) }
        )
    }
}

/**
 * Quick Action Chip
 */
@Composable
private fun QuickActionChip(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

/**
 * Amenity Card Component
 */
@Composable
private fun AmenityCard(
    amenity: Amenity,
    distanceInfo: com.example.flightbooking.data.models.DistanceInfo?,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon
                    Surface(
                        color = getAmenityColor(amenity.type).copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            painter = painterResource(id = getAmenityIcon(amenity.type)),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp),
                            tint = getAmenityColor(amenity.type)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = amenity.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = amenity.type.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (amenity.isOpen) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (amenity.isOpen) "Open" else "Closed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (amenity.isOpen) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = amenity.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Location
                    DetailItem(
                        icon = R.drawable.ic_flight,
                        text = "Level ${amenity.position.floor}"
                    )

                    // Distance
                    distanceInfo?.let {
                        DetailItem(
                            icon = R.drawable.ic_person,
                            text = "${it.meters.toInt()}m • ${it.walkingTimeMinutes} min"
                        )
                    }

                    // Accessibility
                    if (amenity.isAccessible) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = "Accessible",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Navigate Button
                if (distanceInfo != null) {
                    FilledTonalButton(
                        onClick = onNavigateClick,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flight),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate")
                    }
                }
            }
        }
    }
}

/**
 * Detail Item Component
 */
@Composable
private fun DetailItem(
    icon: Int,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Amenity Filter Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmenityFilterSheet(
    selectedType: AmenityType?,
    onTypeSelected: (AmenityType?) -> Unit,
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
            Text(
                text = "Filter by Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // All option
            FilterOption(
                label = "All Amenities",
                isSelected = selectedType == null,
                onClick = { onTypeSelected(null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Category options
            AmenityType.values().forEach { type ->
                FilterOption(
                    label = type.name.replace("_", " "),
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

/**
 * Filter Option Component
 */
@Composable
private fun FilterOption(
    label: String,
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

/**
 * Empty State Component
 */
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Helper Functions
 */
private fun getAmenityIcon(type: AmenityType): Int {
    return when (type) {
        AmenityType.RESTROOM -> R.drawable.ic_person
        AmenityType.FOOD_COURT, AmenityType.RESTAURANT, AmenityType.COFFEE_SHOP -> R.drawable.ic_discount
        AmenityType.LOUNGE -> R.drawable.ic_star
        AmenityType.CHARGING_STATION -> R.drawable.ic_info
        AmenityType.ATM, AmenityType.CURRENCY_EXCHANGE -> R.drawable.ic_work
        AmenityType.PHARMACY, AmenityType.MEDICAL_CENTER -> R.drawable.ic_info
        else -> R.drawable.ic_info
    }
}

private fun getAmenityColor(type: AmenityType): androidx.compose.ui.graphics.Color {
    return when (type) {
        AmenityType.RESTROOM -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        AmenityType.FOOD_COURT, AmenityType.RESTAURANT, AmenityType.COFFEE_SHOP -> 
            androidx.compose.ui.graphics.Color(0xFFFF9800)
        AmenityType.LOUNGE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        AmenityType.CHARGING_STATION -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        AmenityType.ATM, AmenityType.CURRENCY_EXCHANGE -> androidx.compose.ui.graphics.Color(0xFFFFC107)
        AmenityType.PHARMACY, AmenityType.MEDICAL_CENTER -> androidx.compose.ui.graphics.Color(0xFFF44336)
        else -> androidx.compose.ui.graphics.Color.Gray
    }
}

// Made with Bob
