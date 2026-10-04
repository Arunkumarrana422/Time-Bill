package com.example.ui.payments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())

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
                title = { Text("💳 Payments Received (₹${totalReceived.toInt()})", fontWeight = FontWeight.Bold) },
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
            if (paymentsState.value.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No payments recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(paymentsState.value) { payment ->
                    val job = if (payment.jobId.isNotEmpty()) jobsState.value.find { it.jobId == payment.jobId } else null
                    val displayDescription = when {
                        job != null -> "Payment for ${job.serviceName}"
                        payment.notes.isNotBlank() && !payment.notes.startsWith("Payment for job job_") -> payment.notes
                        payment.notes.startsWith("Payment for job job_") -> {
                            val cleanJob = jobsState.value.find { payment.notes.contains(it.jobId) }
                            if (cleanJob != null) "Payment for ${cleanJob.serviceName}" else "Payment for Work"
                        }
                        else -> "Payment for Service"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(payment.customerName.ifBlank { "Customer" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = displayDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Method: ${payment.method} • ${payment.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${payment.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF16A34A))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Received",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Record Payment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
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

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (₹) *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = { Text("Method (Cash, UPI, Bank)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Description / Notes") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                    notes = if (notes.isNotBlank()) notes else "Payment for Service"
                                )
                                repository.savePayment(payment)

                                val cust = selectedCustomer!!
                                val newPaid = cust.paidAmount + amt
                                val newPending = maxOf(0.0, cust.totalAmount - newPaid)
                                val updatedCust = cust.copy(
                                    paidAmount = newPaid,
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
