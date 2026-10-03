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

    // Finish / Review Dialog state
    var showReviewDialog by remember { mutableStateOf(false) }
    var editableRate by remember { mutableStateOf("500") }
    var editableAmount by remember { mutableStateOf("0") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(isTimerStarted, isPaused, isOnBreak) {
        while (isTimerStarted && !isPaused) {
            if (isOnBreak) {
                delay(1000L)
                breakSeconds += 1
            } else {
                delay(1000L)
                currentElapsedSeconds = accumulatedSeconds + ((System.currentTimeMillis() - startTimeMs) / 1000)
            }
        }
    }

    val rate = customRate.toDoubleOrNull() ?: selectedService?.hourlyRate ?: 500.0
    val billableSeconds = maxOf(0L, currentElapsedSeconds - breakSeconds)
    val billableHours = billableSeconds / 3600.0
    val calculatedAmount = billableHours * rate
    val minCharge = selectedService?.minimumCharge ?: 0.0
    val currentAmount = maxOf(calculatedAmount, if (billableSeconds > 0) minCharge else 0.0)

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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isTimerStarted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Customer & Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

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

                        Spacer(modifier = Modifier.height(16.dp))

                        var serviceExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = serviceExpanded,
                            onExpandedChange = { serviceExpanded = !serviceExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedService?.name ?: "Select Service *",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = serviceExpanded,
                                onDismissRequest = { serviceExpanded = false }
                            ) {
                                servicesState.value.forEach { service ->
                                    DropdownMenuItem(
                                        text = { Text("${service.name} (₹${service.hourlyRate}/hr)") },
                                        onClick = {
                                            selectedService = service
                                            customRate = service.hourlyRate.toString()
                                            serviceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customRate,
                            onValueChange = { customRate = it },
                            label = { Text("Hourly Rate (₹) [Editable]") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Job Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
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
                            Text("Start Timer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            text = "Current Amount: ₹${String.format(Locale.getDefault(), "%.2f", currentAmount)}",
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
                            singleLine = true
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
                                val jobId = "job_${System.currentTimeMillis()}"
                                val job = Job(
                                    jobId = jobId,
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
                                repository.saveJob(job)

                                // Update customer totals correctly
                                selectedCustomer?.let { cust ->
                                    val updatedCust = cust.copy(
                                        totalJobs = cust.totalJobs + 1,
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
