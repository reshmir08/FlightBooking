package com.example.flightbooking.ui.screens

import android.app.DatePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightbooking.R
import com.example.flightbooking.data.models.BottomNavItem
import com.example.flightbooking.data.models.Location
import com.example.flightbooking.data.models.PassengerSelection
import com.example.flightbooking.ui.components.BottomNavigationBar
import com.example.flightbooking.ui.components.DateSelector
import com.example.flightbooking.ui.components.FlightInputFields
import com.example.flightbooking.ui.components.PassengerCabinSelector
import com.example.flightbooking.ui.components.TripTypeSelector
import com.example.flightbooking.ui.theme.BackgroundLight
import com.example.flightbooking.ui.theme.CardBackground
import com.example.flightbooking.ui.theme.Warning
import com.example.flightbooking.viewmodel.BookingViewModel
import java.time.LocalDate
import java.util.Calendar

/**
 * Main booking screen
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel = viewModel(),
    onSearchFlights: () -> Unit = {},
    onNavigate: (BottomNavItem) -> Unit = {}
) {
    val bookingState by viewModel.bookingState.collectAsState()
    val searchCriteria = bookingState.searchCriteria
    val context = LocalContext.current
    
    var showDepartureDialog by remember { mutableStateOf(false) }
    var showArrivalDialog by remember { mutableStateOf(false) }
    var showPassengerDialog by remember { mutableStateOf(false) }
    var isDepartureDate by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            BookingTopBar()
        },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = BottomNavItem.BOOK,
                onItemSelected = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Trip type selector
            TripTypeSelector(
                selectedTripType = searchCriteria.tripType,
                onTripTypeSelected = { viewModel.updateTripType(it) }
            )
            
            // Flight input fields (departure, arrival, swap)
            FlightInputFields(
                departure = searchCriteria.departure,
                arrival = searchCriteria.arrival,
                onDepartureClick = { showDepartureDialog = true },
                onArrivalClick = { showArrivalDialog = true },
                onSwapClick = { viewModel.swapLocations() }
            )
            
            // Date selector
            DateSelector(
                departureDate = searchCriteria.departureDate,
                returnDate = searchCriteria.returnDate,
                onDepartureDateClick = {
                    isDepartureDate = true
                    showAndroidDatePicker(
                        context = context,
                        initialDate = searchCriteria.departureDate,
                        onDateSelected = { date ->
                            viewModel.updateDepartureDate(date)
                        }
                    )
                },
                onReturnDateClick = {
                    isDepartureDate = false
                    showAndroidDatePicker(
                        context = context,
                        initialDate = searchCriteria.returnDate ?: searchCriteria.departureDate?.plusDays(7),
                        minDate = searchCriteria.departureDate,
                        onDateSelected = { date ->
                            viewModel.updateReturnDate(date)
                        }
                    )
                }
            )
            
            // Passenger and cabin selector
            PassengerCabinSelector(
                passengers = searchCriteria.passengers,
                cabinClass = searchCriteria.cabinClass,
                onPassengerClick = {
                    showPassengerDialog = true
                },
                onCabinClick = {
                    // TODO: Show cabin picker
                }
            )
            
            // Search Flights Button
            androidx.compose.material3.Button(
                onClick = onSearchFlights,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Search Flights",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Airport Selection Dialogs
        if (showDepartureDialog) {
            AirportSelectionDialog(
                title = "Select Departure Airport",
                onAirportSelected = { airport ->
                    viewModel.updateDeparture(airport)
                    showDepartureDialog = false
                },
                onDismiss = { showDepartureDialog = false }
            )
        }
        
        if (showArrivalDialog) {
            AirportSelectionDialog(
                title = "Select Arrival Airport",
                onAirportSelected = { airport ->
                    viewModel.updateArrival(airport)
                    showArrivalDialog = false
                },
                onDismiss = { showArrivalDialog = false }
            )
        }
        
        // Passenger Selection Dialog
        if (showPassengerDialog) {
            PassengerSelectionDialog(
                currentPassengers = searchCriteria.passengers,
                onPassengersSelected = { adults, children ->
                    viewModel.updatePassengers(adults + children)
                    showPassengerDialog = false
                },
                onDismiss = { showPassengerDialog = false }
            )
        }
    }
}

@Composable
private fun BookingTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.book),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Placeholder for airline logo
            Box(
                modifier = Modifier
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "✈️",
                    fontSize = 24.sp
                )
            }
        }
    }
}

// Made with Bob


/**
 * Airport Selection Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirportSelectionDialog(
    title: String,
    onAirportSelected: (Location) -> Unit,
    onDismiss: () -> Unit
) {
    val airports = listOf(
        Location("YYZ", "Toronto Pearson International", "Toronto"),
        Location("JFK", "John F. Kennedy International", "New York"),
        Location("LAX", "Los Angeles International", "Los Angeles"),
        Location("LHR", "London Heathrow", "London"),
        Location("CDG", "Charles de Gaulle", "Paris"),
        Location("DXB", "Dubai International", "Dubai"),
        Location("SIN", "Singapore Changi", "Singapore"),
        Location("HND", "Tokyo Haneda", "Tokyo"),
        Location("SYD", "Sydney Kingsford Smith", "Sydney"),
        Location("FRA", "Frankfurt Airport", "Frankfurt")
    )
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(airports.size) { index ->
                    val airport = airports[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAirportSelected(airport) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = airport.city,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${airport.name} (${airport.code})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Show Android DatePickerDialog
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun showAndroidDatePicker(
    context: android.content.Context,
    initialDate: LocalDate?,
    minDate: LocalDate? = null,
    onDateSelected: (LocalDate) -> Unit
) {
    val calendar = Calendar.getInstance()
    
    // Set initial date
    if (initialDate != null) {
        calendar.set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    }
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            onDateSelected(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    // Set minimum date if provided
    if (minDate != null) {
        val minCalendar = Calendar.getInstance()
        minCalendar.set(minDate.year, minDate.monthValue - 1, minDate.dayOfMonth)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
    } else {
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
    }
    
    datePickerDialog.show()
}

/**
 * Passenger Selection Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerSelectionDialog(
    currentPassengers: Int,
    onPassengersSelected: (adults: Int, children: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var adults by remember { mutableStateOf(if (currentPassengers > 0) currentPassengers else 1) }
    var children by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Passengers") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Adults
                PassengerCounter(
                    label = "Adults",
                    subtitle = "12+ years",
                    count = adults,
                    onIncrement = { if (adults < 9) adults++ },
                    onDecrement = { if (adults > 1) adults-- },
                    minValue = 1
                )
                
                Divider()
                
                // Children
                PassengerCounter(
                    label = "Children",
                    subtitle = "2-11 years",
                    count = children,
                    onIncrement = { if (children < 9) children++ },
                    onDecrement = { if (children > 0) children-- },
                    minValue = 0
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPassengersSelected(adults, children) }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PassengerCounter(
    label: String,
    subtitle: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    minValue: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDecrement,
                enabled = count > minValue,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Button(
                onClick = onIncrement,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
