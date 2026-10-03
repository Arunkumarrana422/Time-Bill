package com.example.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.TimeBillRepository
import com.example.ui.util.clearFocusOnTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onFinish: () -> Unit
) {
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val servicesState = repository.observeServices(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedService by remember { mutableStateOf<ServiceItem?>(null) }
    var customRate by remember { mutableStateOf("500") }
    var notes by remember { mutableStateOf("") }

    var isTimerStarted by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isOnBreak by remember { mutableStateOf(false) }

    var startTimeMs by remember { mutableStateOf(0L) }
    var accumulatedSeconds by remember { mutableStateOf(0L) }
    var breakSeconds by remember { mutableStateOf(0L) }
    var currentElapsedSeconds by remember { mutableStateOf(0L) }

    // Quick Add Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerMobile by remember { mutableStateOf("") }
    var newCustomerVillage by remember { mutableStateOf("") }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceRate by remember { mutableStateOf("500") }

    // Finish / Review Dialog state
    var showReviewDialog by remember { mutableStateOf(false) }
    var editableRate by remember { mutableStateOf("500") }
    var editableAmount by remember { mutableStateOf("0") }

    val scope = rememberCoroutineScope()

    // Ensure services are seeded
    LaunchedEffect(currentUserId) {
        repository.seedDefaultServicesIfNeeded(currentUserId)
    }

    // Auto-select first service if none selected
    LaunchedEffect(servicesState.value) {
        if (selectedService == null && servicesState.value.isNotEmpty()) {
            val first = servicesState.value.first()
            selectedService = first
            customRate = first.hourlyRate.toString()
        }
    }

    LaunchedEffect(isTimerStarted, isPaused, isOnBreak) {
        while (isTimerStarted && !isPaused) {
            if (isOnBreak) {
                delay(1000L)
                breakSeconds += 1
            } else {
                delay(1000L)
                currentElapsedSeconds += 1
            }
        }
    }

    val rate = customRate.toDoubleOrNull() ?: selectedService?.hourlyRate ?: 500.0
    val billableSeconds = maxOf(0L, currentElapsedSeconds - breakSeconds)
    val billableHours = billableSeconds / 3600.0
    val currentAmount = billableHours * rate

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    val startTimeFormatted = if (startTimeMs > 0L) timeFormat.format(Date(startTimeMs)) else timeFormat.format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏱️ Work Timer & Billing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isTimerStarted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Customer & Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Customer Dropdown + Quick Add
                        var customerExpanded by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = customerExpanded,
                                onExpandedChange = { customerExpanded = !customerExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedCustomer?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Customer *") },
                                    placeholder = { Text("Select Customer") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = customerExpanded,
                                    onDismissRequest = { customerExpanded = false }
                                ) {
                                    if (customersState.value.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No customers found. Click + to add") },
                                            onClick = {
                                                customerExpanded = false
                                                showAddCustomerDialog = true
                                            }
                                        )
                                    } else {
                                        customersState.value.forEach { customer ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(customer.name, fontWeight = FontWeight.SemiBold)
                                                        if (customer.mobile.isNotBlank()) {
                                                            Text(customer.mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedCustomer = customer
                                                    customerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("➕ Add New Customer", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            customerExpanded = false
                                            showAddCustomerDialog = true
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = { showAddCustomerDialog = true },
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Service Dropdown + Quick Add
                        var serviceExpanded by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = serviceExpanded,
                                onExpandedChange = { serviceExpanded = !serviceExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedService?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Service *") },
                                    placeholder = { Text("Select Service") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = serviceExpanded,
                                    onDismissRequest = { serviceExpanded = false }
                                ) {
                                    servicesState.value.forEach { service ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(service.name, fontWeight = FontWeight.Medium)
                                                    Text("₹${service.hourlyRate.toInt()}/hr", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            onClick = {
                                                selectedService = service
                                                customRate = service.hourlyRate.toString()
                                                serviceExpanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("➕ Add Custom Service", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            serviceExpanded = false
                                            showAddServiceDialog = true
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = { showAddServiceDialog = true },
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Service")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customRate,
                            onValueChange = { customRate = it },
                            label = { Text("Hourly Rate (₹) [Editable]") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Job Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (selectedCustomer == null) return@Button
                                startTimeMs = System.currentTimeMillis()
                                isTimerStarted = true
                            },
                            enabled = selectedCustomer != null,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedCustomer == null) "Select Customer First" else "Start Timer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(selectedCustomer?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(selectedService?.name ?: "Working", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(24.dp))

                        val hours = billableSeconds / 3600
                        val minutes = (billableSeconds % 3600) / 60
                        val seconds = billableSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Current Amount: ₹${currentAmount.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rate edit field during active timer
                        OutlinedTextField(
                            value = customRate,
                            onValueChange = { customRate = it },
                            label = { Text("Edit Rate (₹/hr)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Start: $startTimeFormatted")
                            Text("Rate: ₹$rate/hr")
                            if (breakSeconds > 0) {
                                Text("Break: ${breakSeconds / 60}m")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isPaused = !isPaused
                            if (!isPaused) {
                                startTimeMs = System.currentTimeMillis()
                            } else {
                                accumulatedSeconds = currentElapsedSeconds
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "Resume" else "Pause")
                    }

                    Button(
                        onClick = {
                            isOnBreak = !isOnBreak
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isOnBreak) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Coffee, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isOnBreak) "End Break" else "Start Break")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        editableRate = rate.toString()
                        editableAmount = String.format(Locale.getDefault(), "%.2f", currentAmount)
                        showReviewDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finish & Review Bill", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Add Customer Dialog
        if (showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("➕ Add New Customer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("Customer Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newCustomerMobile,
                            onValueChange = { newCustomerMobile = it },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newCustomerVillage,
                            onValueChange = { newCustomerVillage = it },
                            label = { Text("Village / Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCustomerName.isNotBlank()) {
                                scope.launch {
                                    val existing = customersState.value.find { it.name.trim().equals(newCustomerName.trim(), ignoreCase = true) }
                                    val newCust = existing?.copy(
                                        mobile = if (newCustomerMobile.isNotBlank()) newCustomerMobile.trim() else existing.mobile,
                                        village = if (newCustomerVillage.isNotBlank()) newCustomerVillage.trim() else existing.village,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: Customer(
                                        customerId = "cust_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        name = newCustomerName.trim(),
                                        mobile = newCustomerMobile.trim(),
                                        village = newCustomerVillage.trim()
                                    )
                                    repository.saveCustomer(newCust)
                                    selectedCustomer = newCust
                                    newCustomerName = ""
                                    newCustomerMobile = ""
                                    newCustomerVillage = ""
                                    showAddCustomerDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Save Customer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Quick Add Service Dialog
        if (showAddServiceDialog) {
            AlertDialog(
                onDismissRequest = { showAddServiceDialog = false },
                title = { Text("➕ Add New Service") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newServiceName,
                            onValueChange = { newServiceName = it },
                            label = { Text("Service Name * (e.g. Harrowing)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newServiceRate,
                            onValueChange = { newServiceRate = it },
                            label = { Text("Hourly Rate (₹)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newServiceName.isNotBlank()) {
                                val sRate = newServiceRate.toDoubleOrNull() ?: 500.0
                                scope.launch {
                                    val newSrv = ServiceItem(
                                        serviceId = "srv_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        name = newServiceName.trim(),
                                        hourlyRate = sRate
                                    )
                                    repository.saveService(newSrv)
                                    selectedService = newSrv
                                    customRate = sRate.toString()
                                    newServiceName = ""
                                    showAddServiceDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Save Service")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddServiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showReviewDialog) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Review & Confirm Bill") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Customer: ${selectedCustomer?.name}")
                        Text("Service: ${selectedService?.name ?: "Service"}")
                        Text("Billable Time: ${billableSeconds / 3600}h ${(billableSeconds % 3600) / 60}m")

                        OutlinedTextField(
                            value = editableRate,
                            onValueChange = { editableRate = it },
                            label = { Text("Hourly Rate (₹)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editableAmount,
                            onValueChange = { editableAmount = it },
                            label = { Text("Final Amount (₹) [Editable]") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalRate = editableRate.toDoubleOrNull() ?: rate
                            val finalAmt = editableAmount.toDoubleOrNull() ?: currentAmount
                            val totalMin = (currentElapsedSeconds / 60).toInt()
                            val breakMin = (breakSeconds / 60).toInt()
                            val billableMin = maxOf(1, totalMin - breakMin)

                            scope.launch {
                                val existingJob = jobsState.value.find { 
                                    it.customerId == (selectedCustomer?.customerId ?: "") && 
                                    it.serviceId == (selectedService?.serviceId ?: "") && 
                                    it.date == currentDate 
                                }

                                val job = if (existingJob != null) {
                                    existingJob.copy(
                                        totalDurationMinutes = existingJob.totalDurationMinutes + totalMin,
                                        breakDurationMinutes = existingJob.breakDurationMinutes + breakMin,
                                        billableDurationMinutes = existingJob.billableDurationMinutes + billableMin,
                                        baseAmount = existingJob.baseAmount + finalAmt,
                                        finalAmount = existingJob.finalAmount + finalAmt,
                                        pendingAmount = existingJob.pendingAmount + finalAmt,
                                        endTime = timeFormat.format(Date()),
                                        notes = if (notes.isBlank()) existingJob.notes else if (existingJob.notes.isBlank()) notes else "${existingJob.notes}, $notes"
                                    )
                                } else {
                                    Job(
                                        jobId = "job_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        customerId = selectedCustomer?.customerId ?: "cust_unknown",
                                        customerName = selectedCustomer?.name ?: "Unknown",
                                        serviceId = selectedService?.serviceId ?: "srv_unknown",
                                        serviceName = selectedService?.name ?: "Service",
                                        date = currentDate,
                                        startTime = startTimeFormatted,
                                        endTime = timeFormat.format(Date()),
                                        totalDurationMinutes = totalMin,
                                        breakDurationMinutes = breakMin,
                                        billableDurationMinutes = billableMin,
                                        rate = finalRate,
                                        baseAmount = finalAmt,
                                        finalAmount = finalAmt,
                                        pendingAmount = finalAmt,
                                        paymentStatus = "Pending",
                                        status = "Completed",
                                        notes = notes
                                    )
                                }
                                repository.saveJob(job)

                                // Update customer totals correctly
                                selectedCustomer?.let { cust ->
                                    val updatedCust = cust.copy(
                                        totalJobs = if (existingJob == null) cust.totalJobs + 1 else cust.totalJobs,
                                        totalAmount = cust.totalAmount + finalAmt,
                                        pendingAmount = cust.pendingAmount + finalAmt,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    repository.saveCustomer(updatedCust)
                                }

                                showReviewDialog = false
                                onFinish()
                            }
                        }
                    ) {
                        Text("Confirm & Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
