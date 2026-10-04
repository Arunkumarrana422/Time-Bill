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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun PaymentListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedJobForPayment by remember { mutableStateOf<Job?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💰 Payment History", fontWeight = FontWeight.Bold) },
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
                onClick = {
                    selectedCustomer = null
                    selectedJobForPayment = null
                    amount = ""
                    notes = ""
                    showAddDialog = true
                },
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
                title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Customer Dropdown
                        var customerExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Customer *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
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
                                            selectedJobForPayment = null
                                            customerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Service / Job Dropdown if customer selected
                        if (selectedCustomer != null) {
                            val custJobs = jobsState.value.filter { it.customerId == selectedCustomer!!.customerId && it.pendingAmount > 0 }
                            if (custJobs.isNotEmpty()) {
                                var jobDropdownExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = jobDropdownExpanded,
                                    onExpandedChange = { jobDropdownExpanded = it }
                                ) {
                                    val jobTitle = if (selectedJobForPayment != null) {
                                        "${selectedJobForPayment!!.serviceName} (${selectedJobForPayment!!.date}) - ₹${selectedJobForPayment!!.pendingAmount.toInt()} Due"
                                    } else {
                                        "General / All Services"
                                    }

                                    OutlinedTextField(
                                        value = jobTitle,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Service / Job") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = jobDropdownExpanded,
                                        onDismissRequest = { jobDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("General / All Services", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                selectedJobForPayment = null
                                                jobDropdownExpanded = false
                                            }
                                        )
                                        custJobs.forEach { j ->
                                            DropdownMenuItem(
                                                text = { Text("${j.serviceName} (${j.date}) - ₹${j.pendingAmount.toInt()} Due") },
                                                onClick = {
                                                    selectedJobForPayment = j
                                                    amount = j.pendingAmount.toInt().toString()
                                                    notes = "Payment for ${j.serviceName}"
                                                    jobDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
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

                        // Payment Method Dropdown
                        var methodExpanded by remember { mutableStateOf(false) }
                        val methods = listOf("Cash", "UPI / Online", "Bank Transfer", "Cheque", "Other")
                        ExposedDropdownMenuBox(
                            expanded = methodExpanded,
                            onExpandedChange = { methodExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = method,
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
                                            method = m
                                            methodExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Service Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cust = selectedCustomer ?: return@Button
                            val amt = round(amount.toDoubleOrNull() ?: 0.0)
                            if (amt <= 0) return@Button

                            scope.launch {
                                val payId = "pay_${System.currentTimeMillis()}"
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val chosenJobId = selectedJobForPayment?.jobId ?: ""
                                val paymentDesc = if (notes.isNotBlank()) {
                                    notes
                                } else if (selectedJobForPayment != null) {
                                    "Payment for ${selectedJobForPayment!!.serviceName}"
                                } else {
                                    "Payment for Services"
                                }

                                val payment = Payment(
                                    paymentId = payId,
                                    userId = currentUserId,
                                    customerId = cust.customerId,
                                    customerName = cust.name,
                                    jobId = chosenJobId,
                                    amount = amt,
                                    method = method,
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
                                    val unpaidJobs = jobsState.value.filter { it.customerId == cust.customerId && it.pendingAmount > 0 }.sortedBy { it.createdAt }
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

                                val customerJobs = jobsState.value.filter { it.customerId == cust.customerId }
                                val totalBilled = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else cust.totalAmount
                                val newCustomerPaid = cust.paidAmount + amt
                                val newCustomerPending = maxOf(0.0, totalBilled - newCustomerPaid)
                                val updatedCustomer = cust.copy(
                                    paidAmount = newCustomerPaid,
                                    pendingAmount = newCustomerPending,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(updatedCustomer)

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
