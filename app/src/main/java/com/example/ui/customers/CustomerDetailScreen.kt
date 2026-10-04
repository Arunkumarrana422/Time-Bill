package com.example.ui.customers

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    currentUserId: String,
    customerId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    val customer = customersState.value.find { it.customerId == customerId }
    val customerJobs = jobsState.value.filter { it.customerId == customerId }
    val customerPayments = paymentsState.value.filter { it.customerId == customerId }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var payAmount by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("Cash") }
    var payNotes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalBilling = customerJobs.sumOf { it.finalAmount }
    val totalPaid = customerPayments.sumOf { it.amount } + customerJobs.sumOf { it.paidAmount }
    val effectivePaid = maxOf(customer.paidAmount, totalPaid)
    val pendingDue = maxOf(0.0, totalBilling - effectivePaid)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                    }
                    IconButton(onClick = {
                        val msg = "Hello ${customer.name}, your total bill is ₹${totalBilling.toInt()}, Total Paid: ₹${effectivePaid.toInt()}, and Pending Due: ₹${pendingDue.toInt()} for Time Bill work. Please make payment at your earliest convenience. Thank you!"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${customer.mobile}&text=${Uri.encode(msg)}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Reminder", tint = Color(0xFF25D366))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            if (pendingDue > 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        payAmount = pendingDue.toInt().toString()
                        showPaymentDialog = true
                    },
                    icon = { Icon(Icons.Default.Payment, contentDescription = null) },
                    text = { Text("Receive Payment") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Ledger Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ledger Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📞 Mobile: ${customer.mobile}")
                        if (customer.village.isNotEmpty()) Text("📍 Village / Location: ${customer.village}")
                        if (customer.notes.isNotEmpty()) Text("📝 Notes: ${customer.notes}")
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Billing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalBilling.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Paid", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A))
                                Text("₹${effectivePaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16A34A))
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Pending Due", style = MaterialTheme.typography.bodySmall, color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                                Text(
                                    text = if (pendingDue > 0) "₹${pendingDue.toInt()}" else "✓ All Paid",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }

            // Job History Section
            item {
                Text("Job History (${customerJobs.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (customerJobs.isEmpty()) {
                item {
                    Text("No jobs found for this customer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(customerJobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.serviceName, fontWeight = FontWeight.Bold)
                                Text("${job.date} • ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${job.finalAmount.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (job.paymentStatus == "Paid") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = job.paymentStatus,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = if (job.paymentStatus == "Paid") Color(0xFF166534) else Color(0xFF991B1B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment History Section
            item {
                Text("Payment History (${customerPayments.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (customerPayments.isEmpty()) {
                item {
                    Text("No payments recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(customerPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (payment.notes.isNotBlank() && !payment.notes.contains("job_")) payment.notes else "Payment via ${payment.method}", fontWeight = FontWeight.Bold)
                                Text("${payment.method} • ${payment.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("₹${payment.amount.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Receive Payment from ${customer.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            label = { Text("Amount (₹) *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = payMethod,
                            onValueChange = { payMethod = it },
                            label = { Text("Payment Method (Cash, UPI, Bank)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = payNotes,
                            onValueChange = { payNotes = it },
                            label = { Text("Notes (Optional)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = payAmount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) return@Button
                            scope.launch {
                                val payId = "pay_${System.currentTimeMillis()}"
                                val payment = Payment(
                                    paymentId = payId,
                                    userId = currentUserId,
                                    customerId = customer.customerId,
                                    customerName = customer.name,
                                    amount = amt,
                                    method = payMethod,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    notes = if (payNotes.isNotBlank()) payNotes else "Payment received"
                                )
                                repository.savePayment(payment)

                                val newPaid = effectivePaid + amt
                                val newPending = maxOf(0.0, totalBilling - newPaid)
                                val updatedCust = customer.copy(
                                    paidAmount = newPaid,
                                    pendingAmount = newPending,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(updatedCust)

                                showPaymentDialog = false
                            }
                        }
                    ) {
                        Text("Confirm Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
