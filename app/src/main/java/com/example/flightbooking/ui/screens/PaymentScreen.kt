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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flightbooking.R
import com.example.flightbooking.data.models.Flight
import com.example.flightbooking.data.models.PaymentInfo
import com.example.flightbooking.data.models.PaymentMethod
import java.text.NumberFormat
import java.util.*

/**
 * Payment Screen
 * Handles payment information and booking confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    flight: Flight,
    totalPrice: Double,
    onConfirmPayment: (PaymentInfo) -> Unit,
    onBackClick: () -> Unit
) {
    var paymentInfo by remember { mutableStateOf(PaymentInfo()) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CREDIT_CARD) }
    var agreeToTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Amount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatPrice(totalPrice),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            onConfirmPayment(
                                paymentInfo.copy(
                                    paymentMethod = selectedPaymentMethod,
                                    amount = totalPrice
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                        enabled = isPaymentInfoValid(paymentInfo, selectedPaymentMethod) && agreeToTerms
                    ) {
                        Text("Confirm & Pay")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Booking Summary
            BookingSummaryCard(flight = flight, totalPrice = totalPrice)

            Divider()

            // Payment Method Selection
            PaymentMethodSection(
                selectedMethod = selectedPaymentMethod,
                onMethodSelected = { selectedPaymentMethod = it }
            )

            Divider()

            // Payment Details Form
            when (selectedPaymentMethod) {
                PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> {
                    CardPaymentForm(
                        paymentInfo = paymentInfo,
                        onPaymentInfoChange = { paymentInfo = it }
                    )
                }
                PaymentMethod.PAYPAL -> {
                    PayPalPaymentForm(
                        paymentInfo = paymentInfo,
                        onPaymentInfoChange = { paymentInfo = it }
                    )
                }
                PaymentMethod.BANK_TRANSFER -> {
                    BankTransferForm(
                        paymentInfo = paymentInfo,
                        onPaymentInfoChange = { paymentInfo = it }
                    )
                }
                PaymentMethod.CRYPTO -> {
                    CryptoPaymentForm(
                        paymentInfo = paymentInfo,
                        onPaymentInfoChange = { paymentInfo = it }
                    )
                }
            }

            Divider()

            // Billing Address
            BillingAddressSection(
                paymentInfo = paymentInfo,
                onPaymentInfoChange = { paymentInfo = it }
            )

            Divider()

            // Terms and Conditions
            TermsAndConditionsSection(
                agreed = agreeToTerms,
                onAgreedChange = { agreeToTerms = it }
            )

            // Security Notice
            SecurityNoticeCard()
        }
    }
}

/**
 * Booking Summary Card
 */
@Composable
private fun BookingSummaryCard(
    flight: Flight,
    totalPrice: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Booking Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${flight.segments.first().origin.code} → ${flight.segments.last().destination.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = flight.airline.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Flight ${flight.flightNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = flight.cabinClass.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPrice(totalPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Payment Method Selection Section
 */
@Composable
private fun PaymentMethodSection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        PaymentMethod.values().forEach { method ->
            PaymentMethodOption(
                method = method,
                isSelected = selectedMethod == method,
                onClick = { onMethodSelected(method) }
            )
        }
    }
}

/**
 * Payment Method Option
 */
@Composable
private fun PaymentMethodOption(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder()
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = getPaymentMethodIcon(method)),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = getPaymentMethodName(method),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

/**
 * Card Payment Form
 */
@Composable
private fun CardPaymentForm(
    paymentInfo: PaymentInfo,
    onPaymentInfoChange: (PaymentInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Card Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = paymentInfo.cardNumber,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(cardNumber = it)) },
            label = { Text("Card Number *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("1234 5678 9012 3456") }
        )

        OutlinedTextField(
            value = paymentInfo.cardHolderName,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(cardHolderName = it)) },
            label = { Text("Cardholder Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("JOHN DOE") }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = paymentInfo.expiryDate,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(expiryDate = it)) },
                label = { Text("Expiry Date *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("MM/YY") }
            )

            OutlinedTextField(
                value = paymentInfo.cvv,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(cvv = it)) },
                label = { Text("CVV *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = { Text("123") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = paymentInfo.saveCard,
                onCheckedChange = { onPaymentInfoChange(paymentInfo.copy(saveCard = it)) }
            )
            Text(
                text = "Save card for future bookings",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * PayPal Payment Form
 */
@Composable
private fun PayPalPaymentForm(
    paymentInfo: PaymentInfo,
    onPaymentInfoChange: (PaymentInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PayPal Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = paymentInfo.paypalEmail,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(paypalEmail = it)) },
            label = { Text("PayPal Email *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "You'll be redirected to PayPal to complete your payment securely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Bank Transfer Form
 */
@Composable
private fun BankTransferForm(
    paymentInfo: PaymentInfo,
    onPaymentInfoChange: (PaymentInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bank Account Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = paymentInfo.bankAccountNumber,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(bankAccountNumber = it)) },
            label = { Text("Account Number *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        OutlinedTextField(
            value = paymentInfo.bankRoutingNumber,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(bankRoutingNumber = it)) },
            label = { Text("Routing Number *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        OutlinedTextField(
            value = paymentInfo.bankName,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(bankName = it)) },
            label = { Text("Bank Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

/**
 * Crypto Payment Form
 */
@Composable
private fun CryptoPaymentForm(
    paymentInfo: PaymentInfo,
    onPaymentInfoChange: (PaymentInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cryptocurrency Payment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = paymentInfo.cryptoWalletAddress,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(cryptoWalletAddress = it)) },
            label = { Text("Wallet Address *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Supported Cryptocurrencies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Bitcoin (BTC)\n• Ethereum (ETH)\n• USDT\n• USDC",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Billing Address Section
 */
@Composable
private fun BillingAddressSection(
    paymentInfo: PaymentInfo,
    onPaymentInfoChange: (PaymentInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Billing Address",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = paymentInfo.billingAddress,
            onValueChange = { onPaymentInfoChange(paymentInfo.copy(billingAddress = it)) },
            label = { Text("Address *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = paymentInfo.billingCity,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(billingCity = it)) },
                label = { Text("City *") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = paymentInfo.billingState,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(billingState = it)) },
                label = { Text("State *") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = paymentInfo.billingZipCode,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(billingZipCode = it)) },
                label = { Text("ZIP Code *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = paymentInfo.billingCountry,
                onValueChange = { onPaymentInfoChange(paymentInfo.copy(billingCountry = it)) },
                label = { Text("Country *") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

/**
 * Terms and Conditions Section
 */
@Composable
private fun TermsAndConditionsSection(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = agreed,
            onCheckedChange = onAgreedChange
        )
        Column {
            Text(
                text = "I agree to the Terms and Conditions *",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "By checking this box, you agree to our booking terms, cancellation policy, and privacy policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Security Notice Card
 */
@Composable
private fun SecurityNoticeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "Secure Payment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your payment information is encrypted and secure. We never store your full card details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Helper Functions
 */
private fun getPaymentMethodIcon(method: PaymentMethod): Int {
    return when (method) {
        PaymentMethod.CREDIT_CARD -> R.drawable.ic_work
        PaymentMethod.DEBIT_CARD -> R.drawable.ic_work
        PaymentMethod.PAYPAL -> R.drawable.ic_info
        PaymentMethod.BANK_TRANSFER -> R.drawable.ic_work
        PaymentMethod.CRYPTO -> R.drawable.ic_star
    }
}

private fun getPaymentMethodName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.CREDIT_CARD -> "Credit Card"
        PaymentMethod.DEBIT_CARD -> "Debit Card"
        PaymentMethod.PAYPAL -> "PayPal"
        PaymentMethod.BANK_TRANSFER -> "Bank Transfer"
        PaymentMethod.CRYPTO -> "Cryptocurrency"
    }
}

private fun isPaymentInfoValid(paymentInfo: PaymentInfo, method: PaymentMethod): Boolean {
    val billingValid = paymentInfo.billingAddress.isNotEmpty() &&
            paymentInfo.billingCity.isNotEmpty() &&
            paymentInfo.billingState.isNotEmpty() &&
            paymentInfo.billingZipCode.isNotEmpty() &&
            paymentInfo.billingCountry.isNotEmpty()

    return when (method) {
        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> {
            billingValid &&
                    paymentInfo.cardNumber.isNotEmpty() &&
                    paymentInfo.cardHolderName.isNotEmpty() &&
                    paymentInfo.expiryDate.isNotEmpty() &&
                    paymentInfo.cvv.isNotEmpty()
        }
        PaymentMethod.PAYPAL -> {
            billingValid && paymentInfo.paypalEmail.isNotEmpty()
        }
        PaymentMethod.BANK_TRANSFER -> {
            billingValid &&
                    paymentInfo.bankAccountNumber.isNotEmpty() &&
                    paymentInfo.bankRoutingNumber.isNotEmpty() &&
                    paymentInfo.bankName.isNotEmpty()
        }
        PaymentMethod.CRYPTO -> {
            billingValid && paymentInfo.cryptoWalletAddress.isNotEmpty()
        }
    }
}

private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(price)
}

// Made with Bob
