package com.example.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.util.clearFocusOnTap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SetupScreen(
    currentUserId: String,
    initialProfile: UserProfile?,
    onSaveComplete: (UserProfile) -> Unit
) {
    val firebaseUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }

    var name by remember(initialProfile, firebaseUser) { 
        mutableStateOf(
            initialProfile?.name?.takeIf { it.isNotBlank() } 
                ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() } 
                ?: ""
        ) 
    }
    var businessName by remember(initialProfile) { 
        mutableStateOf(
            initialProfile?.businessName?.takeIf { it.isNotBlank() } ?: ""
        ) 
    }
    var mobile by remember(initialProfile, firebaseUser) { 
        mutableStateOf(
            initialProfile?.mobile?.takeIf { it.isNotBlank() } 
                ?: firebaseUser?.phoneNumber?.takeIf { it.isNotBlank() } 
                ?: ""
        ) 
    }
    var address by remember(initialProfile) { mutableStateOf(initialProfile?.address ?: "") }
    var currency by remember(initialProfile) { mutableStateOf(initialProfile?.currency ?: "₹") }
    var defaultRate by remember { mutableStateOf(initialProfile?.defaultRate?.toInt()?.toString() ?: "500") }
    var defaultService by remember { mutableStateOf(initialProfile?.defaultService ?: "Tractor Ploughing") }
    var invoicePrefix by remember { mutableStateOf(initialProfile?.invoicePrefix ?: "INV") }
    var paymentTerms by remember { mutableStateOf(initialProfile?.paymentTerms ?: "Due on receipt") }

    var isSaving by remember { mutableStateOf(false) }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .clearFocusOnTap(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Business Setup Wizard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure your profile and billing defaults",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            val setupCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(setupCardShape),
                shape = setupCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    val isNameError = setupErrorMessage != null && name.isBlank()
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            if (setupErrorMessage != null) setupErrorMessage = null
                        },
                        label = { Text("Owner / User Name *") },
                        isError = isNameError,
                        supportingText = {
                            if (isNameError) {
                                Text("Owner name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isBusinessError = setupErrorMessage != null && businessName.isBlank()
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { 
                            businessName = it
                            if (setupErrorMessage != null) setupErrorMessage = null
                        },
                        label = { Text("Business Name *") },
                        isError = isBusinessError,
                        supportingText = {
                            if (isBusinessError) {
                                Text("Business name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isMobileError = (setupErrorMessage != null && (mobile.isBlank() || mobile.length != 10)) || (mobile.isNotEmpty() && mobile.length < 10)
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(10)
                            mobile = digits
                            if (setupErrorMessage != null) setupErrorMessage = null
                        },
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = isMobileError,
                        supportingText = {
                            if (isMobileError) {
                                val err = if (mobile.isBlank()) "Mobile number is required *" else "Mobile number must be 10 digits (${mobile.length}/10)"
                                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Village / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = defaultRate,
                            onValueChange = { defaultRate = it },
                            label = { Text("Hourly Rate (₹)") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultService,
                        onValueChange = { defaultService = it },
                        label = { Text("Default Service") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = paymentTerms,
                            onValueChange = { paymentTerms = it },
                            label = { Text("Payment Terms") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (setupErrorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = setupErrorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedBusiness = businessName.trim()
                            val trimmedMobile = mobile.trim()

                            if (trimmedName.isBlank()) {
                                setupErrorMessage = "Please enter owner / user name *"
                                return@Button
                            }
                            if (trimmedBusiness.isBlank()) {
                                setupErrorMessage = "Please enter business name *"
                                return@Button
                            }
                            if (trimmedMobile.isBlank() || trimmedMobile.length != 10) {
                                setupErrorMessage = "Mobile number must be a valid 10-digit number *"
                                return@Button
                            }

                            isSaving = true
                            val profile = UserProfile(
                                userId = currentUserId,
                                name = trimmedName,
                                businessName = trimmedBusiness,
                                mobile = trimmedMobile,
                                address = address.trim(),
                                currency = currency.ifBlank { "₹" },
                                defaultRate = defaultRate.toDoubleOrNull() ?: 500.0,
                                defaultService = defaultService.trim().ifBlank { "Tractor Ploughing" },
                                invoicePrefix = invoicePrefix.trim().ifBlank { "INV" },
                                paymentTerms = paymentTerms.trim().ifBlank { "Due on receipt" },
                                isSetupComplete = true
                            )
                            onSaveComplete(profile)
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text("Save & Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
