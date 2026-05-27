package com.example.flightbooking.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.flightbooking.R
import com.example.flightbooking.data.models.BottomNavItem
import com.example.flightbooking.ui.theme.IconTint

/**
 * Bottom navigation bar with four tabs
 */
@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        contentColor = IconTint
    ) {
        // 1. Book
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Book"
                )
            },
            label = { Text("Book") },
            selected = selectedItem == BottomNavItem.BOOK,
            onClick = { onItemSelected(BottomNavItem.BOOK) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        
        // 2. Airport
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flight),
                    contentDescription = "Airport"
                )
            },
            label = { Text("Airport") },
            selected = selectedItem == BottomNavItem.AIRPORT,
            onClick = { onItemSelected(BottomNavItem.AIRPORT) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IconTint,
                selectedTextColor = IconTint,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        
        // 3. Profile
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            selected = selectedItem == BottomNavItem.PROFILE,
            onClick = { onItemSelected(BottomNavItem.PROFILE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IconTint,
                selectedTextColor = IconTint,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        
        // 4. Settings
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            selected = selectedItem == BottomNavItem.SETTINGS,
            onClick = { onItemSelected(BottomNavItem.SETTINGS) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IconTint,
                selectedTextColor = IconTint,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

// Made with Bob
