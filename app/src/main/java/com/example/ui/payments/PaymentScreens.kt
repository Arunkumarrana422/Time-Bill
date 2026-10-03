package com.example.ui.payments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<com.example.data.model.Customer?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val totalReceived = paymentsState.value.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💳 Payments Received (₹$totalReceived)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Payment")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(paymentsState.value) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(payment.customerName.ifBlank { "Customer" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Method: ${payment.method} • ${payment.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (payment.notes.isNotEmpty()) {
                                Text(payment.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("₹${payment.amount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Record Payment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var customerExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = !customerExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Select Customer *",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                customersState.value.forEach { customer ->
                                    DropdownMenuItem(
                                        text = { Text(customer.name) },
                                        onClick = {
                                            selectedCustomer = customer
                                            customerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹) *") }, singleLine = true)
                        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method (Cash, UPI, Bank)") }, singleLine = true)
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (selectedCustomer == null || amt <= 0) return@Button
                            scope.launch {
                                val payId = "pay_${System.currentTimeMillis()}"
                                val payment = Payment(
                                    paymentId = payId,
                                    userId = currentUserId,
                                    customerId = selectedCustomer!!.customerId,
                                    customerName = selectedCustomer!!.name,
                                    amount = amt,
                                    method = method,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    notes = notes
                                )
                                repository.savePayment(payment)

                                val cust = selectedCustomer!!
                                val newPending = maxOf(0.0, cust.pendingAmount - amt)
                                val updatedCust = cust.copy(
                                    pendingAmount = newPending,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(updatedCust)

                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
