package com.example.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
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

    // Quick Add Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerMobile by remember { mutableStateOf("") }
    var newCustomerVillage by remember { mutableStateOf("") }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceRate by remember { mutableStateOf("500") }

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
            rate = first.hourlyRate.toString()
        }
    }

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
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                            rate = service.hourlyRate.toString()
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

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { hours = it },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { minutes = it },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = breakMinutes,
                            onValueChange = { breakMinutes = it },
                            label = { Text("Break (Mins)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = { Text("Rate / Hour (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = additionalCharges,
                            onValueChange = { additionalCharges = it },
                            label = { Text("Addl Charges (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Discount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider()

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
                        Text(
                            text = if (selectedCustomer == null) "Select Customer First" else "Save Job & Bill",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                                    val newCust = Customer(
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
                                        hourlyRate = sRate,
                                        minimumCharge = sRate / 2
                                    )
                                    repository.saveService(newSrv)
                                    selectedService = newSrv
                                    rate = sRate.toString()
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
    }
}
