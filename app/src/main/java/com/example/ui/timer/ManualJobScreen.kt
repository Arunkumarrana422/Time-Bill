package com.example.ui.timer

import android.app.DatePickerDialog
import com.example.ui.util.formatIndianCurrency
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.TimeBillRepository
import com.example.ui.util.clearFocusOnTap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualJobScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val servicesState = repository.observeServices(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var selectedService by remember { mutableStateOf<ServiceItem?>(null) }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("500") }
    var additionalCharges by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
            rate = first.hourlyRate.toInt().toString()
        }
    }

    val hrs = hours.toDoubleOrNull() ?: 0.0
    val mins = minutes.toDoubleOrNull() ?: 0.0
    val totalMin = (hrs * 60 + mins).toInt()
    val hourlyRate = round(rate.toDoubleOrNull() ?: 500.0)
    val baseAmt = round((totalMin / 60.0) * hourlyRate)
    val addl = round(additionalCharges.toDoubleOrNull() ?: 0.0)
    val finalAmt = maxOf(0.0, baseAmt + addl)

    val matchingCustomers = customersState.value.filter {
        customerSearchQuery.isBlank() ||
                it.name.contains(customerSearchQuery, ignoreCase = true) ||
                it.mobile.contains(customerSearchQuery) ||
                it.village.contains(customerSearchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Job Entry", fontWeight = FontWeight.Bold) },
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
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Searchable Customer Dropdown + Quick Add
                    var customerExpanded by remember { mutableStateOf(false) }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = !customerExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (customerSearchQuery.isNotEmpty()) customerSearchQuery else (selectedCustomer?.name ?: ""),
                                onValueChange = { query ->
                                    customerSearchQuery = query
                                    selectedCustomer = customersState.value.find { it.name.equals(query.trim(), ignoreCase = true) }
                                    customerExpanded = true
                                },
                                readOnly = false,
                                singleLine = true,
                                label = { Text("Customer *", maxLines = 1) },
                                placeholder = { Text("Search customer...", maxLines = 1) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (customerSearchQuery.isNotEmpty() || selectedCustomer != null) {
                                            IconButton(onClick = {
                                                customerSearchQuery = ""
                                                selectedCustomer = null
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                if (matchingCustomers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No customer found matching '$customerSearchQuery'") },
                                        onClick = {
                                            newCustomerName = customerSearchQuery
                                            customerExpanded = false
                                            showAddCustomerDialog = true
                                        }
                                    )
                                } else {
                                    matchingCustomers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(customer.name, fontWeight = FontWeight.SemiBold)
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(
                                                            text = customer.mobile,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        if (customer.village.isNotEmpty()) {
                                                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(
                                                                text = customer.village,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedCustomer = customer
                                                customerSearchQuery = customer.name
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
                                singleLine = true,
                                label = { Text("Service *", maxLines = 1) },
                                placeholder = { Text("Select Service", maxLines = 1) },
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
                                                Text("₹${formatIndianCurrency(service.hourlyRate)}/hr", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            selectedService = service
                                            rate = service.hourlyRate.toInt().toString()
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

                    Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            singleLine = true,
                            label = { Text("Date (YYYY-MM-DD)", maxLines = 1) },
                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Pick Date") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

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
                        Text("${totalMin / 60}h ${totalMin % 60}m", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("₹${finalAmt.toLong()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedCustomer == null || selectedService == null) return@Button
                            scope.launch {
                                val existingJob = jobsState.value.find { 
                                    it.customerId == selectedCustomer!!.customerId && 
                                    it.serviceId == selectedService!!.serviceId && 
                                    it.date == date 
                                }

                                val job = if (existingJob != null) {
                                    existingJob.copy(
                                        totalDurationMinutes = existingJob.totalDurationMinutes + totalMin,
                                        breakDurationMinutes = 0,
                                        billableDurationMinutes = existingJob.billableDurationMinutes + totalMin,
                                        baseAmount = existingJob.baseAmount + baseAmt,
                                        additionalChargesAmount = existingJob.additionalChargesAmount + addl,
                                        discountAmount = 0.0,
                                        finalAmount = existingJob.finalAmount + finalAmt,
                                        pendingAmount = existingJob.pendingAmount + finalAmt,
                                        notes = if (notes.isBlank()) existingJob.notes else if (existingJob.notes.isBlank()) notes else "${existingJob.notes}, $notes"
                                    )
                                } else {
                                    Job(
                                        jobId = "job_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        customerId = selectedCustomer!!.customerId,
                                        customerName = selectedCustomer!!.name,
                                        serviceId = selectedService!!.serviceId,
                                        serviceName = selectedService!!.name,
                                        date = date,
                                        startTime = "08:00 AM",
                                        endTime = "05:00 PM",
                                        totalDurationMinutes = totalMin,
                                        breakDurationMinutes = 0,
                                        billableDurationMinutes = totalMin,
                                        rate = hourlyRate,
                                        baseAmount = baseAmt,
                                        additionalChargesAmount = addl,
                                        discountAmount = 0.0,
                                        finalAmount = finalAmt,
                                        pendingAmount = finalAmt,
                                        paymentStatus = "Pending",
                                        status = "Completed",
                                        notes = notes
                                    )
                                }
                                repository.saveJob(job)

                                val cust = selectedCustomer!!
                                val updatedCust = cust.copy(
                                    totalJobs = if (existingJob == null) cust.totalJobs + 1 else cust.totalJobs,
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

        // Quick Add Customer Dialog (without address field)
        if (showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("Add New Customer", fontWeight = FontWeight.Bold) },
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
                            label = { Text("Village / Location") },
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
                                    customerSearchQuery = newCust.name
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
                title = { Text("Add New Service", fontWeight = FontWeight.Bold) },
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
                                val sRate = round(newServiceRate.toDoubleOrNull() ?: 500.0)
                                scope.launch {
                                    val newSrv = ServiceItem(
                                        serviceId = "srv_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        name = newServiceName.trim(),
                                        hourlyRate = sRate,
                                        minuteRate = sRate / 60.0
                                    )
                                    repository.saveService(newSrv)
                                    selectedService = newSrv
                                    rate = sRate.toInt().toString()
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
