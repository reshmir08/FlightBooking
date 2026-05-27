package com.example.flightbooking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.BottomNavItem

/**
 * Airport Navigation Hub Screen
 * Central hub for all airport navigation features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportHubScreen(
    onNavigateToTerminalMap: () -> Unit,
    onNavigateToGateFinder: () -> Unit,
    onNavigateToAmenities: () -> Unit,
    onNavigateToNavigation: () -> Unit,
    onNavigate: (BottomNavItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Airport Navigation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            com.example.flightbooking.ui.components.BottomNavigationBar(
                selectedItem = BottomNavItem.AIRPORT,
                onItemSelected = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✈️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Welcome to Airport Services",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Navigate the airport with ease",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(getAirportFeatures()) { feature ->
                    AirportFeatureCard(
                        feature = feature,
                        onClick = {
                            when (feature.id) {
                                "terminal_map" -> onNavigateToTerminalMap()
                                "gate_finder" -> onNavigateToGateFinder()
                                "amenities" -> onNavigateToAmenities()
                                "navigation" -> onNavigateToNavigation()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Airport Feature Card
 */
@Composable
private fun AirportFeatureCard(
    feature: AirportFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = feature.color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = feature.icon,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Data class for airport features
 */
private data class AirportFeature(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val color: Color
)

/**
 * Get list of airport features
 */
private fun getAirportFeatures(): List<AirportFeature> {
    return listOf(
        AirportFeature(
            id = "terminal_map",
            icon = "🗺️",
            title = "Terminal Map",
            description = "Interactive terminal layout",
            color = Color(0xFF2196F3)
        ),
        AirportFeature(
            id = "gate_finder",
            icon = "🚪",
            title = "Gate Finder",
            description = "Find your gate quickly",
            color = Color(0xFF4CAF50)
        ),
        AirportFeature(
            id = "amenities",
            icon = "🍽️",
            title = "Amenities",
            description = "Restaurants, shops & more",
            color = Color(0xFFFF9800)
        ),
        AirportFeature(
            id = "navigation",
            icon = "🧭",
            title = "Navigation",
            description = "Turn-by-turn directions",
            color = Color(0xFF9C27B0)
        )
    )
}

// Made with Bob