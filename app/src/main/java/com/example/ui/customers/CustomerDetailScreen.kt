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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

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
    var selectedJobForPayment by remember { mutableStateOf<Job?>(null) }
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

    val totalBilling = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else customer.totalAmount
    val paymentsSum = customerPayments.sumOf { it.amount }
    val jobsPaidSum = customerJobs.sumOf { it.paidAmount }
    
    // Accurate single-counted total paid
    val effectivePaid = if (customerPayments.isNotEmpty()) {
        paymentsSum
    } else if (jobsPaidSum > 0) {
        jobsPaidSum
    } else {
        customer.paidAmount
    }
    
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
                        val firstPendingJob = customerJobs.find { it.pendingAmount > 0 }
                        selectedJobForPayment = firstPendingJob
                        payAmount = (firstPendingJob?.pendingAmount ?: pendingDue).toInt().toString()
                        payNotes = if (firstPendingJob != null) "Payment for ${firstPendingJob.serviceName}" else "General Payment"
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
                val summaryCardShape = RoundedCornerShape(16.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(summaryCardShape),
                    shape = summaryCardShape,
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
                    val jobItemShape = RoundedCornerShape(14.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(jobItemShape),
                        shape = jobItemShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    color = if (job.paymentStatus == "Paid" || job.pendingAmount <= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (job.paymentStatus == "Paid" || job.pendingAmount <= 0) "Paid" else "Pending: ₹${job.pendingAmount.toInt()}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = if (job.paymentStatus == "Paid" || job.pendingAmount <= 0) Color(0xFF166534) else Color(0xFF991B1B),
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
                    val paymentCardShape = RoundedCornerShape(14.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(paymentCardShape),
                        shape = paymentCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

        // Receive Payment Dialog with Service Selection Dropdown
        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Receive Payment from ${customer.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Service / Job Dropdown
                        var serviceDropdownExpanded by remember { mutableStateOf(false) }
                        val pendingJobs = customerJobs.filter { it.pendingAmount > 0 }

                        ExposedDropdownMenuBox(
                            expanded = serviceDropdownExpanded,
                            onExpandedChange = { serviceDropdownExpanded = it }
                        ) {
                            val serviceTitle = if (selectedJobForPayment != null) {
                                "${selectedJobForPayment!!.serviceName} (${selectedJobForPayment!!.date}) - ₹${selectedJobForPayment!!.pendingAmount.toInt()} Due"
                            } else {
                                "All / General Settlement (₹${pendingDue.toInt()} Due)"
                            }

                            OutlinedTextField(
                                value = serviceTitle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Service / Job *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = serviceDropdownExpanded,
                                onDismissRequest = { serviceDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All / General Settlement (₹${pendingDue.toInt()})", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedJobForPayment = null
                                        payAmount = pendingDue.toInt().toString()
                                        payNotes = "General Payment"
                                        serviceDropdownExpanded = false
                                    }
                                )
                                pendingJobs.forEach { job ->
                                    DropdownMenuItem(
                                        text = { Text("${job.serviceName} (${job.date}) - ₹${job.pendingAmount.toInt()} Pending") },
                                        onClick = {
                                            selectedJobForPayment = job
                                            payAmount = job.pendingAmount.toInt().toString()
                                            payNotes = "Payment for ${job.serviceName}"
                                            serviceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            label = { Text("Payment Amount (₹) *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Payment Method Dropdown
                        var methodExpanded by remember { mutableStateOf(false) }
                        val methods = listOf("Cash", "UPI / Online", "Bank Transfer", "Cheque", "Other")
                        ExposedDropdownMenuBox(
                            expanded = methodExpanded,
                            onExpandedChange = { methodExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = payMethod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Payment Method") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = methodExpanded,
                                onDismissRequest = { methodExpanded = false }
                            ) {
                                methods.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            payMethod = m
                                            methodExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = payNotes,
                            onValueChange = { payNotes = it },
                            label = { Text("Payment Notes / Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = round(payAmount.toDoubleOrNull() ?: 0.0)
                            if (amt <= 0) return@Button
                            scope.launch {
                                val payId = "pay_${System.currentTimeMillis()}"
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                
                                val chosenJobId = selectedJobForPayment?.jobId ?: ""
                                val paymentDesc = if (payNotes.isNotBlank()) {
                                    payNotes
                                } else if (selectedJobForPayment != null) {
                                    "Payment for ${selectedJobForPayment!!.serviceName}"
                                } else {
                                    "Payment for Services"
                                }

                                val payment = Payment(
                                    paymentId = payId,
                                    userId = currentUserId,
                                    customerId = customerId,
                                    customerName = customer.name,
                                    jobId = chosenJobId,
                                    amount = amt,
                                    method = payMethod,
                                    date = today,
                                    notes = paymentDesc
                                )
                                repository.savePayment(payment)

                                if (selectedJobForPayment != null) {
                                    val targetJob = selectedJobForPayment!!
                                    val newJobPaid = targetJob.paidAmount + amt
                                    val newJobPending = maxOf(0.0, targetJob.finalAmount - newJobPaid)
                                    val updatedJob = targetJob.copy(
                                        paidAmount = newJobPaid,
                                        pendingAmount = newJobPending,
                                        paymentStatus = if (newJobPending <= 0) "Paid" else "Partially Paid"
                                    )
                                    repository.saveJob(updatedJob)
                                } else {
                                    var remainingPay = amt
                                    val unpaidJobs = customerJobs.filter { it.pendingAmount > 0 }.sortedBy { it.createdAt }
                                    for (j in unpaidJobs) {
                                        if (remainingPay <= 0) break
                                        val canPay = minOf(remainingPay, j.pendingAmount)
                                        val newPaid = j.paidAmount + canPay
                                        val newPending = maxOf(0.0, j.finalAmount - newPaid)
                                        val updatedJ = j.copy(
                                            paidAmount = newPaid,
                                            pendingAmount = newPending,
                                            paymentStatus = if (newPending <= 0) "Paid" else "Partially Paid"
                                        )
                                        repository.saveJob(updatedJ)
                                        remainingPay -= canPay
                                    }
                                }

                                val newCustomerPaid = effectivePaid + amt
                                val newCustomerPending = maxOf(0.0, totalBilling - newCustomerPaid)
                                val updatedCustomer = customer.copy(
                                    paidAmount = newCustomerPaid,
                                    pendingAmount = newCustomerPending,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(updatedCustomer)

                                showPaymentDialog = false
                            }
                        }
                    ) {
                        Text("Save Payment")
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
