package com.example.flightbooking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.MockData
import com.example.flightbooking.data.models.Gate
import com.example.flightbooking.data.models.GateStatus
import com.example.flightbooking.data.models.Position
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateFinderScreen(
    onNavigateToGate: (Gate) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<GateStatus?>(null) }
    var sortByDistance by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Mock current position (could be from GPS/beacon in real app)
    val currentPosition = remember { Position(300f, 250f, 1) }
    
    // Get all gates
    val allGates = remember { MockData.getSampleGates() }
    
    // Filter and sort gates
    val filteredGates = remember(searchQuery, selectedStatus, sortByDistance, allGates) {
        var gates = allGates
        
        // Filter by search query
        if (searchQuery.isNotBlank()) {
            gates = gates.filter { gate ->
                // Clean search query: remove "gate" prefix and trim
                val cleanQuery = searchQuery.trim()
                    .replace("gate", "", ignoreCase = true)
                    .trim()
                
                // Search in gate number (with and without "gate" prefix)
                gate.number.contains(cleanQuery, ignoreCase = true) ||
                "gate ${gate.number}".contains(searchQuery, ignoreCase = true) ||
                // Search in flight number
                gate.currentFlight?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        // Filter by status
        if (selectedStatus != null) {
            gates = gates.filter { it.status == selectedStatus }
        }
        
        // Sort by distance if enabled
        if (sortByDistance) {
            gates = gates.sortedBy { gate ->
                calculateDistance(currentPosition, gate.position)
            }
        }
        
        gates
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gate Finder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Text("⚙", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search gate or flight number...") },
                leadingIcon = { Text("🔍") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort by distance chip
                FilterChip(
                    selected = sortByDistance,
                    onClick = { sortByDistance = !sortByDistance },
                    label = { Text("📍 Near Me") }
                )
                
                // Status filter chips
                FilterChip(
                    selected = selectedStatus == GateStatus.BOARDING,
                    onClick = { 
                        selectedStatus = if (selectedStatus == GateStatus.BOARDING) null 
                        else GateStatus.BOARDING 
                    },
                    label = { Text("Boarding") }
                )
                
                FilterChip(
                    selected = selectedStatus == GateStatus.DELAYED,
                    onClick = { 
                        selectedStatus = if (selectedStatus == GateStatus.DELAYED) null 
                        else GateStatus.DELAYED 
                    },
                    label = { Text("Delayed") }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Results count
            Text(
                text = "${filteredGates.size} gates found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Gates list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredGates) { gate ->
                    GateCard(
                        gate = gate,
                        currentPosition = currentPosition,
                        onNavigate = { onNavigateToGate(gate) }
                    )
                }
                
                if (filteredGates.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔍",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Text(
                                    text = "No gates found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Try adjusting your search or filters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Filter menu dropdown
        if (showFilterMenu) {
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false },
                modifier = Modifier.padding(16.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("All Gates") },
                    onClick = {
                        selectedStatus = null
                        showFilterMenu = false
                    }
                )
                Divider()
                GateStatus.values().forEach { status ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(getStatusColor(status))
                                )
                                Text(status.name.replace("_", " "))
                            }
                        },
                        onClick = {
                            selectedStatus = status
                            showFilterMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GateCard(
    gate: Gate,
    currentPosition: Position,
    onNavigate: () -> Unit
) {
    val distance = calculateDistance(currentPosition, gate.position)
    val walkingTime = (distance / 80).toInt() // Assuming 80m/min walking speed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigate),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Gate number and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gate number
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gate.number,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Gate ${gate.number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Terminal ${gate.terminal} • Floor ${gate.position.floor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status badge
                StatusBadge(status = gate.status)
            }
            
            // Flight info (if available)
            if (gate.currentFlight != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Flight ${gate.currentFlight}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (gate.boardingTime != null) {
                            Text(
                                text = "Boarding: ${gate.boardingTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = "✈️",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            
            // Distance and navigation
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${distance.toInt()}m away",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "~$walkingTime min walk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onNavigate,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Navigate")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("→")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: GateStatus) {
    val (text, color) = when (status) {
        GateStatus.AVAILABLE -> "Available" to Color(0xFF4CAF50)
        GateStatus.BOARDING -> "Boarding" to Color(0xFF2196F3)
        GateStatus.DELAYED -> "Delayed" to Color(0xFFF44336)
        GateStatus.DEPARTED -> "Departed" to Color(0xFF9E9E9E)
        GateStatus.MAINTENANCE -> "Maintenance" to Color(0xFFFF9800)
        GateStatus.CANCELLED -> "Cancelled" to Color(0xFFE91E63)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun getStatusColor(status: GateStatus): Color {
    return when (status) {
        GateStatus.AVAILABLE -> Color(0xFF4CAF50)
        GateStatus.BOARDING -> Color(0xFF2196F3)
        GateStatus.DELAYED -> Color(0xFFF44336)
        GateStatus.DEPARTED -> Color(0xFF9E9E9E)
        GateStatus.MAINTENANCE -> Color(0xFFFF9800)
        GateStatus.CANCELLED -> Color(0xFFE91E63)
    }
}

private fun calculateDistance(from: Position, to: Position): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dz = (to.floor - from.floor) * 50f // Assume 50m per floor
    return sqrt(dx * dx + dy * dy + dz * dz)
}

// Made with Bob
