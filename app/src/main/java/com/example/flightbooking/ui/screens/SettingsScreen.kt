package com.example.flightbooking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flightbooking.data.models.BottomNavItem

/**
 * Settings Screen
 * User preferences and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (BottomNavItem) -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
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
                selectedItem = BottomNavItem.SETTINGS,
                onItemSelected = onNavigate
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Section
            item {
                ProfileSection()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Preferences Section
            item {
                SectionHeader(title = "Preferences")
            }

            item {
                SettingsSwitchItem(
                    icon = "🔔",
                    title = "Notifications",
                    description = "Flight updates and reminders",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = "🌙",
                    title = "Dark Mode",
                    description = "Use dark theme",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = "📍",
                    title = "Location Services",
                    description = "For airport navigation",
                    checked = locationEnabled,
                    onCheckedChange = { locationEnabled = it }
                )
            }

            // Account Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Account")
            }

            items(getAccountSettings()) { setting ->
                SettingsItem(
                    icon = setting.icon,
                    title = setting.title,
                    description = setting.description,
                    onClick = { /* Handle click */ }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "About")
            }

            items(getAboutSettings()) { setting ->
                SettingsItem(
                    icon = setting.icon,
                    title = setting.title,
                    description = setting.description,
                    onClick = { /* Handle click */ }
                )
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Handle logout */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

/**
 * Profile Section
 */
@Composable
private fun ProfileSection() {
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "John Doe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "john.doe@example.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "Aeroplan Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            // Edit Button
            TextButton(onClick = { /* Handle edit */ }) {
                Text("Edit")
            }
        }
    }
}

/**
 * Section Header
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Settings Item with Switch
 */
@Composable
private fun SettingsSwitchItem(
    icon: String,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Settings Item (clickable)
 */
@Composable
private fun SettingsItem(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Data class for settings items
 */
private data class SettingItem(
    val icon: String,
    val title: String,
    val description: String
)

/**
 * Get account settings
 */
private fun getAccountSettings(): List<SettingItem> {
    return listOf(
        SettingItem("👤", "Personal Information", "Name, email, phone"),
        SettingItem("💳", "Payment Methods", "Manage cards and payment"),
        SettingItem("✈️", "Travel Preferences", "Seat, meal preferences"),
        SettingItem("🎫", "Aeroplan Account", "Points and rewards")
    )
}

/**
 * Get about settings
 */
private fun getAboutSettings(): List<SettingItem> {
    return listOf(
        SettingItem("ℹ️", "About", "Version 1.0.0"),
        SettingItem("📄", "Terms & Conditions", "Legal information"),
        SettingItem("🔒", "Privacy Policy", "How we use your data"),
        SettingItem("💬", "Help & Support", "Contact us")
    )
}

// Made with Bob