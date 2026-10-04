package com.example.ui.payments

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import com.example.ui.util.clearFocusOnTap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePaymentScreen(
    currentUserId: String,
    initialCustomerId: String = "",
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedJobForPayment by remember { mutableStateOf<Job?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("Cash") }
    var payDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var payNotes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Initialize selected customer from passed ID or default
    LaunchedEffect(customersState.value, initialCustomerId) {
        if (selectedCustomer == null && customersState.value.isNotEmpty()) {
            val found = customersState.value.find { it.customerId == initialCustomerId }
            if (found != null) {
                selectedCustomer = found
            }
        }
    }

    // Recalculate customer financials whenever customer or jobs change
    val customerJobs = remember(selectedCustomer, jobsState.value) {
        if (selectedCustomer != null) jobsState.value.filter { it.customerId == selectedCustomer!!.customerId } else emptyList()
    }
    val customerPayments = remember(selectedCustomer, paymentsState.value) {
        if (selectedCustomer != null) paymentsState.value.filter { it.customerId == selectedCustomer!!.customerId } else emptyList()
    }

    val totalBilling = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else (selectedCustomer?.totalAmount ?: 0.0)
    val paymentsSum = customerPayments.sumOf { it.amount }
    val jobsPaidSum = customerJobs.sumOf { it.paidAmount }
    val effectivePaid = if (customerPayments.isNotEmpty()) paymentsSum else if (jobsPaidSum > 0) jobsPaidSum else (selectedCustomer?.paidAmount ?: 0.0)
    val pendingDue = maxOf(0.0, totalBilling - effectivePaid)

    val pendingJobs = remember(customerJobs) {
        customerJobs.filter { it.pendingAmount > 0 }
    }

    // Auto set default amount once customer is loaded and amount is empty
    LaunchedEffect(selectedCustomer, pendingJobs, pendingDue) {
        if (selectedCustomer != null && payAmount.isEmpty()) {
            val firstPending = pendingJobs.firstOrNull()
            selectedJobForPayment = firstPending
            val defaultAmt = (firstPending?.pendingAmount ?: pendingDue).toInt()
            if (defaultAmt > 0) {
                payAmount = defaultAmt.toString()
            }
            if (payNotes.isEmpty()) {
                payNotes = if (firstPending != null) "Payment for ${firstPending.serviceName}" else "General Settlement"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Customer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (selectedCustomer != null && initialCustomerId.isNotBlank()) {
                        // Fixed Customer Details (No dropdown, strictly displaying current customer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedCustomer!!.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedCustomer!!.mobile,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selectedCustomer!!.village.isNotEmpty()) {
                                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedCustomer!!.village,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Only show dropdown when opened from general payment without pre-selected customer
                        var customerDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Select Customer",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Customer *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                customersState.value.forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(cust.name, fontWeight = FontWeight.Bold)
                                                Text(cust.mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = cust
                                            selectedJobForPayment = null
                                            payAmount = ""
                                            payNotes = ""
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Bill", style = MaterialTheme.typography.labelSmall)
                                    Text("₹${totalBilling.toInt()}", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A))
                                    Text("₹${effectivePaid.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pending Due", style = MaterialTheme.typography.labelSmall, color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                                    Text(
                                        text = if (pendingDue > 0) "₹${pendingDue.toInt()}" else "✓ All Paid",
                                        fontWeight = FontWeight.Bold,
                                        color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Details Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Payment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Job / Service Dropdown
                    var serviceDropdownExpanded by remember { mutableStateOf(false) }
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
                            label = { Text("Service / Job Linked") },
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
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
                                    payNotes = "General Settlement"
                                    serviceDropdownExpanded = false
                                }
                            )
                            pendingJobs.forEach { job ->
                                DropdownMenuItem(
                                    text = { Text("${job.serviceName} (${job.date}) - ₹${job.pendingAmount.toInt()} Due") },
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

                    // Payment Amount
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = {
                            payAmount = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text("Payment Amount (₹) *") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Date
                    OutlinedTextField(
                        value = payDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Date") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        payDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        payDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Payment Method Dropdown
                    var methodDropdownExpanded by remember { mutableStateOf(false) }
                    val methods = listOf("Cash", "UPI / Online", "Bank Transfer", "Cheque", "Other")
                    ExposedDropdownMenuBox(
                        expanded = methodDropdownExpanded,
                        onExpandedChange = { methodDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = payMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        payMethod = m
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment Notes
                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Notes / Remarks (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        errorMessage = "Please select a customer."
                        return@Button
                    }
                    val amt = round(payAmount.toDoubleOrNull() ?: 0.0)
                    if (amt <= 0) {
                        errorMessage = "Please enter a valid payment amount."
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        val cust = selectedCustomer!!
                        val payId = "pay_${System.currentTimeMillis()}"
                        val chosenJobId = selectedJobForPayment?.jobId ?: ""
                        val paymentDesc = if (payNotes.isNotBlank()) {
                            payNotes.trim()
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
                            method = payMethod,
                            date = payDate,
                            notes = paymentDesc
                        )
                        repository.savePayment(payment)

                        // If single job targeted
                        if (selectedJobForPayment != null) {
                            val targetJob = selectedJobForPayment!!
                            val newJobPaid = targetJob.paidAmount + amt
                            val newJobPending = maxOf(0.0, targetJob.finalAmount - newJobPaid)
                            val updatedJob = targetJob.copy(
                                paidAmount = newJobPaid,
                                pendingAmount = newJobPending,
                                paymentStatus = if (newJobPending <= 0.0) "Paid" else if (newJobPaid > 0) "Partially Paid" else "Pending"
                            )
                            repository.saveJob(updatedJob)
                        } else {
                            // Automatically distribute across unpaid jobs chronologically
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
                                    paymentStatus = if (newPending <= 0.0) "Paid" else if (newPaid > 0) "Partially Paid" else "Pending"
                                )
                                repository.saveJob(updatedJ)
                                remainingPay -= canPay
                            }
                        }

                        // Update customer ledger
                        val newCustomerPaid = effectivePaid + amt
                        val newCustomerPending = maxOf(0.0, totalBilling - newCustomerPaid)
                        val updatedCustomer = cust.copy(
                            paidAmount = newCustomerPaid,
                            pendingAmount = newCustomerPending,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveCustomer(updatedCustomer)

                        isSaving = false
                        onBack()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
