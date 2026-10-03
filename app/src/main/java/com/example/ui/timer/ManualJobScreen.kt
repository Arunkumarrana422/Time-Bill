package com.example.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualJobScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onFinish: () -> Unit
) {
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val servicesState = repository.observeServices(currentUserId).collectAsState(initial = emptyList())

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedService by remember { mutableStateOf<ServiceItem?>(null) }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var hours by remember { mutableStateOf("2") }
    var minutes by remember { mutableStateOf("30") }
    var breakMinutes by remember { mutableStateOf("0") }
    var rate by remember { mutableStateOf("500") }
    var additionalCharges by remember { mutableStateOf("0") }
    var discount by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val hrs = hours.toDoubleOrNull() ?: 0.0
    val mins = minutes.toDoubleOrNull() ?: 0.0
    val totalMin = (hrs * 60 + mins).toInt()
    val breakMin = breakMinutes.toIntOrNull() ?: 0
    val billableMin = maxOf(0, totalMin - breakMin)
    val hourlyRate = rate.toDoubleOrNull() ?: 500.0
    val baseAmt = (billableMin / 60.0) * hourlyRate
    val addl = additionalCharges.toDoubleOrNull() ?: 0.0
    val disc = discount.toDoubleOrNull() ?: 0.0
    val finalAmt = maxOf(0.0, baseAmt + addl - disc)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Manual Job Entry", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Customer Dropdown
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

                    // Service Dropdown
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
                                    text = { Text(service.name) },
                                    onClick = {
                                        selectedService = service
                                        rate = service.hourlyRate.toString()
                                        serviceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { hours = it },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { minutes = it },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = breakMinutes,
                            onValueChange = { breakMinutes = it },
                            label = { Text("Break (Mins)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = { Text("Rate / Hour (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = additionalCharges,
                            onValueChange = { additionalCharges = it },
                            label = { Text("Addl Charges (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Discount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Billable Time:", fontWeight = FontWeight.Medium)
                        Text("${billableMin / 60}h ${billableMin % 60}m", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", finalAmt)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedCustomer == null || selectedService == null) return@Button
                            scope.launch {
                                val jobId = "job_${System.currentTimeMillis()}"
                                val job = Job(
                                    jobId = jobId,
                                    userId = currentUserId,
                                    customerId = selectedCustomer!!.customerId,
                                    customerName = selectedCustomer!!.name,
                                    serviceId = selectedService!!.serviceId,
                                    serviceName = selectedService!!.name,
                                    date = date,
                                    startTime = "08:00 AM",
                                    endTime = "05:00 PM",
                                    totalDurationMinutes = totalMin,
                                    breakDurationMinutes = breakMin,
                                    billableDurationMinutes = billableMin,
                                    rate = hourlyRate,
                                    baseAmount = baseAmt,
                                    additionalChargesAmount = addl,
                                    discountAmount = disc,
                                    finalAmount = finalAmt,
                                    pendingAmount = finalAmt,
                                    paymentStatus = "Pending",
                                    status = "Completed",
                                    notes = notes
                                )
                                repository.saveJob(job)

                                val cust = selectedCustomer!!
                                val updatedCust = cust.copy(
                                    totalJobs = cust.totalJobs + 1,
                                    totalAmount = cust.totalAmount + finalAmt,
                                    pendingAmount = cust.pendingAmount + finalAmt,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(updatedCust)

                                onFinish()
                            }
                        },
                        enabled = selectedCustomer != null && selectedService != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Job & Bill", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
