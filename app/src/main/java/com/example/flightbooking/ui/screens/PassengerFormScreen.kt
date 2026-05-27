package com.example.flightbooking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.data.models.ContactInfo
import com.example.flightbooking.data.models.PassengerInfo
import java.time.LocalDate

/**
 * Passenger Form Screen
 * Collects passenger and contact information for booking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerFormScreen(
    passengerCount: Int,
    onContinue: (List<PassengerInfo>, ContactInfo) -> Unit,
    onBackClick: () -> Unit
) {
    var passengers by remember {
        mutableStateOf(List(passengerCount) { PassengerInfo() })
    }
    var contactInfo by remember { mutableStateOf(ContactInfo()) }
    var currentStep by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Passenger Information")
                        Text(
                            text = "Step ${currentStep + 1} of ${passengerCount + 1}",
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < passengerCount) {
                                currentStep++
                            } else {
                                onContinue(passengers, contactInfo)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        enabled = if (currentStep < passengerCount) {
                            isPassengerValid(passengers[currentStep])
                        } else {
                            isContactInfoValid(contactInfo)
                        }
                    ) {
                        Text(if (currentStep < passengerCount) "Next" else "Continue to Payment")
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
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / (passengerCount + 1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            if (currentStep < passengerCount) {
                // Passenger form
                PassengerForm(
                    passengerNumber = currentStep + 1,
                    passenger = passengers[currentStep],
                    onPassengerChange = { updatedPassenger ->
                        passengers = passengers.toMutableList().apply {
                            set(currentStep, updatedPassenger)
                        }
                    }
                )
            } else {
                // Contact information form
                ContactInfoForm(
                    contactInfo = contactInfo,
                    onContactInfoChange = { contactInfo = it }
                )
            }
        }
    }
}

/**
 * Passenger Form Component
 */
@Composable
private fun PassengerForm(
    passengerNumber: Int,
    passenger: PassengerInfo,
    onPassengerChange: (PassengerInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Passenger $passengerNumber",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Title
        Text(
            text = "Title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Mr", "Mrs", "Ms", "Dr").forEach { title ->
                FilterChip(
                    selected = passenger.title == title,
                    onClick = { onPassengerChange(passenger.copy(title = title)) },
                    label = { Text(title) }
                )
            }
        }

        // First Name
        OutlinedTextField(
            value = passenger.firstName,
            onValueChange = { onPassengerChange(passenger.copy(firstName = it)) },
            label = { Text("First Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Middle Name
        OutlinedTextField(
            value = passenger.middleName,
            onValueChange = { onPassengerChange(passenger.copy(middleName = it)) },
            label = { Text("Middle Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Last Name
        OutlinedTextField(
            value = passenger.lastName,
            onValueChange = { onPassengerChange(passenger.copy(lastName = it)) },
            label = { Text("Last Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Date of Birth
        Text(
            text = "Date of Birth *",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day
            OutlinedTextField(
                value = passenger.dateOfBirth?.dayOfMonth?.toString() ?: "",
                onValueChange = { day ->
                    if (day.isEmpty() || day.toIntOrNull() != null) {
                        val dayInt = day.toIntOrNull()
                        if (dayInt != null && dayInt in 1..31) {
                            val currentDob = passenger.dateOfBirth ?: LocalDate.now()
                            onPassengerChange(
                                passenger.copy(
                                    dateOfBirth = currentDob.withDayOfMonth(dayInt)
                                )
                            )
                        }
                    }
                },
                label = { Text("Day") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Month
            OutlinedTextField(
                value = passenger.dateOfBirth?.monthValue?.toString() ?: "",
                onValueChange = { month ->
                    if (month.isEmpty() || month.toIntOrNull() != null) {
                        val monthInt = month.toIntOrNull()
                        if (monthInt != null && monthInt in 1..12) {
                            val currentDob = passenger.dateOfBirth ?: LocalDate.now()
                            onPassengerChange(
                                passenger.copy(
                                    dateOfBirth = currentDob.withMonth(monthInt)
                                )
                            )
                        }
                    }
                },
                label = { Text("Month") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Year
            OutlinedTextField(
                value = passenger.dateOfBirth?.year?.toString() ?: "",
                onValueChange = { year ->
                    if (year.isEmpty() || year.toIntOrNull() != null) {
                        val yearInt = year.toIntOrNull()
                        if (yearInt != null && yearInt in 1900..LocalDate.now().year) {
                            val currentDob = passenger.dateOfBirth ?: LocalDate.now()
                            onPassengerChange(
                                passenger.copy(
                                    dateOfBirth = currentDob.withYear(yearInt)
                                )
                            )
                        }
                    }
                },
                label = { Text("Year") },
                modifier = Modifier.weight(1.5f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        // Gender
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Male", "Female", "Other").forEach { gender ->
                FilterChip(
                    selected = passenger.gender == gender,
                    onClick = { onPassengerChange(passenger.copy(gender = gender)) },
                    label = { Text(gender) }
                )
            }
        }

        // Nationality
        OutlinedTextField(
            value = passenger.nationality,
            onValueChange = { onPassengerChange(passenger.copy(nationality = it)) },
            label = { Text("Nationality *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Passport Number
        OutlinedTextField(
            value = passenger.passportNumber,
            onValueChange = { onPassengerChange(passenger.copy(passportNumber = it)) },
            label = { Text("Passport Number *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Passport Expiry
        OutlinedTextField(
            value = passenger.passportExpiry?.toString() ?: "",
            onValueChange = { /* Date picker would be better */ },
            label = { Text("Passport Expiry (YYYY-MM-DD) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("2025-12-31") }
        )

        // Frequent Flyer Number
        OutlinedTextField(
            value = passenger.frequentFlyerNumber,
            onValueChange = { onPassengerChange(passenger.copy(frequentFlyerNumber = it)) },
            label = { Text("Frequent Flyer Number (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Special Requests
        OutlinedTextField(
            value = passenger.specialRequests,
            onValueChange = { onPassengerChange(passenger.copy(specialRequests = it)) },
            label = { Text("Special Requests (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            placeholder = { Text("Meal preferences, wheelchair assistance, etc.") }
        )

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ensure all information matches your travel documents exactly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Contact Information Form Component
 */
@Composable
private fun ContactInfoForm(
    contactInfo: ContactInfo,
    onContactInfoChange: (ContactInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Contact Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "We'll send your booking confirmation and updates to this contact information.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Email
        OutlinedTextField(
            value = contactInfo.email,
            onValueChange = { onContactInfoChange(contactInfo.copy(email = it)) },
            label = { Text("Email Address *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null
                )
            }
        )

        // Confirm Email
        OutlinedTextField(
            value = contactInfo.confirmEmail,
            onValueChange = { onContactInfoChange(contactInfo.copy(confirmEmail = it)) },
            label = { Text("Confirm Email *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = contactInfo.confirmEmail.isNotEmpty() && contactInfo.email != contactInfo.confirmEmail,
            supportingText = {
                if (contactInfo.confirmEmail.isNotEmpty() && contactInfo.email != contactInfo.confirmEmail) {
                    Text("Emails do not match")
                }
            }
        )

        // Phone Number
        OutlinedTextField(
            value = contactInfo.phone,
            onValueChange = { onContactInfoChange(contactInfo.copy(phone = it)) },
            label = { Text("Phone Number *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null
                )
            },
            placeholder = { Text("+1 (555) 123-4567") }
        )

        // Country Code
        OutlinedTextField(
            value = contactInfo.countryCode,
            onValueChange = { onContactInfoChange(contactInfo.copy(countryCode = it)) },
            label = { Text("Country Code *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("US") }
        )

        // Address
        OutlinedTextField(
            value = contactInfo.address,
            onValueChange = { onContactInfoChange(contactInfo.copy(address = it)) },
            label = { Text("Address *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // City
        OutlinedTextField(
            value = contactInfo.city,
            onValueChange = { onContactInfoChange(contactInfo.copy(city = it)) },
            label = { Text("City *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // State/Province
        OutlinedTextField(
            value = contactInfo.state,
            onValueChange = { onContactInfoChange(contactInfo.copy(state = it)) },
            label = { Text("State/Province *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Postal Code
        OutlinedTextField(
            value = contactInfo.postalCode,
            onValueChange = { onContactInfoChange(contactInfo.copy(postalCode = it)) },
            label = { Text("Postal Code *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Emergency Contact
        Text(
            text = "Emergency Contact (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = contactInfo.emergencyContactName,
            onValueChange = { onContactInfoChange(contactInfo.copy(emergencyContactName = it)) },
            label = { Text("Emergency Contact Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = contactInfo.emergencyContactPhone,
            onValueChange = { onContactInfoChange(contactInfo.copy(emergencyContactPhone = it)) },
            label = { Text("Emergency Contact Phone") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

        // Newsletter subscription
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = contactInfo.subscribeToNewsletter,
                onCheckedChange = {
                    onContactInfoChange(contactInfo.copy(subscribeToNewsletter = it))
                }
            )
            Text(
                text = "Send me exclusive deals and travel tips",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Validate passenger information
 */
private fun isPassengerValid(passenger: PassengerInfo): Boolean {
    return passenger.title.isNotEmpty() &&
            passenger.firstName.isNotEmpty() &&
            passenger.lastName.isNotEmpty() &&
            passenger.dateOfBirth != null &&
            passenger.gender.isNotEmpty() &&
            passenger.nationality.isNotEmpty() &&
            passenger.passportNumber.isNotEmpty() &&
            passenger.passportExpiry != null
}

/**
 * Validate contact information
 */
private fun isContactInfoValid(contactInfo: ContactInfo): Boolean {
    return contactInfo.email.isNotEmpty() &&
            contactInfo.email == contactInfo.confirmEmail &&
            contactInfo.phone.isNotEmpty() &&
            contactInfo.countryCode.isNotEmpty() &&
            contactInfo.address.isNotEmpty() &&
            contactInfo.city.isNotEmpty() &&
            contactInfo.state.isNotEmpty() &&
            contactInfo.postalCode.isNotEmpty() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(contactInfo.email).matches()
}

// Made with Bob
