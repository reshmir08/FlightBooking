package com.example.flightbooking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.data.models.TripType
import com.example.flightbooking.ui.theme.ChipBorder
import com.example.flightbooking.ui.theme.SelectedChipBackground
import com.example.flightbooking.ui.theme.UnselectedChipBackground

/**
 * Trip type selector component with two options: Round-trip, One-way
 */
@Composable
fun TripTypeSelector(
    selectedTripType: TripType,
    onTripTypeSelected: (TripType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TripTypeChip(
            text = stringResource(R.string.round_trip),
            selected = selectedTripType == TripType.ROUND_TRIP,
            onClick = { onTripTypeSelected(TripType.ROUND_TRIP) }
        )
        
        TripTypeChip(
            text = stringResource(R.string.one_way),
            selected = selectedTripType == TripType.ONE_WAY,
            onClick = { onTripTypeSelected(TripType.ONE_WAY) }
        )
    }
}

@Composable
private fun TripTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = UnselectedChipBackground,
            selectedContainerColor = SelectedChipBackground,
            labelColor = Color.Black,
            selectedLabelColor = Color.White
        ),
        border = if (!selected) {
            BorderStroke(1.dp, ChipBorder)
        } else {
            null
        }
    )
}

// Made with Bob
