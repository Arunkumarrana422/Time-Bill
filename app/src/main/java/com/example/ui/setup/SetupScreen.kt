package com.example.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Owner / User Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number *") },
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

                    Button(
                        onClick = {
                            val profile = UserProfile(
                                userId = currentUserId,
                                name = name.ifBlank { "Owner" },
                                businessName = businessName.ifBlank { "My Business" },
                                mobile = mobile,
                                address = address,
                                currency = currency.ifBlank { "₹" },
                                defaultRate = defaultRate.toDoubleOrNull() ?: 500.0,
                                defaultService = defaultService,
                                invoicePrefix = invoicePrefix,
                                paymentTerms = paymentTerms,
                                isSetupComplete = true
                            )
                            onSaveComplete(profile)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
