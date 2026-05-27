package com.example.flightbooking.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.ui.theme.CardBackground
import com.example.flightbooking.ui.theme.IconTint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Date selector component for selecting departure and return dates
 */
@Composable
fun DateSelector(
    departureDate: LocalDate?,
    returnDate: LocalDate?,
    onDateClick: () -> Unit = {},
    onDepartureDateClick: () -> Unit = {},
    onReturnDateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Departure Date Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDepartureDateClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = "Departure Date",
                    tint = IconTint,
                    modifier = Modifier.size(20.dp)
                )
                
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = "Departure",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = departureDate?.format(DateTimeFormatter.ofPattern("MMM dd")) ?: "Select",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (departureDate == null) Color.Gray else IconTint
                    )
                }
            }
        }
        
        // Return Date Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onReturnDateClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = "Return Date",
                    tint = IconTint,
                    modifier = Modifier.size(20.dp)
                )
                
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = "Return",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = returnDate?.format(DateTimeFormatter.ofPattern("MMM dd")) ?: "Select",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (returnDate == null) Color.Gray else IconTint
                    )
                }
            }
        }
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(departureDate: LocalDate?, returnDate: LocalDate?): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    
    return when {
        departureDate != null && returnDate != null -> {
            "${departureDate.format(formatter)} - ${returnDate.format(formatter)}"
        }
        departureDate != null -> {
            departureDate.format(formatter)
        }
        else -> "Select dates"
    }
}

// Made with Bob
