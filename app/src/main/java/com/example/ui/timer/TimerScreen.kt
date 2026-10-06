package com.example.ui.timer

import android.Manifest
import com.example.ui.util.formatIndianCurrency
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.TimeBillRepository
import com.example.service.TimerStateManager
import com.example.ui.util.clearFocusOnTap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Request notification permission for foreground timer notification on Android 13+
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val servicesState = repository.observeServices(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())

    val activeTimerData by TimerStateManager.timerData.collectAsState()
    val liveElapsedSeconds by TimerStateManager.elapsedSeconds.collectAsState()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var selectedService by remember { mutableStateOf<ServiceItem?>(null) }
    var customRate by remember { mutableStateOf("500") }
    var notes by remember { mutableStateOf("") }

    // Quick Add Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerMobile by remember { mutableStateOf("") }
    var newCustomerVillage by remember { mutableStateOf("") }
    var customerMobileError by remember { mutableStateOf<String?>(null) }
    var customerNameError by remember { mutableStateOf<String?>(null) }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceRate by remember { mutableStateOf("500") }
    var serviceNameError by remember { mutableStateOf<String?>(null) }
    var isSavingCustomerDialog by remember { mutableStateOf(false) }
    var isSavingServiceDialog by remember { mutableStateOf(false) }

    // Finish / Review Dialog state
    var showReviewDialog by remember { mutableStateOf(false) }
    var editableRate by remember { mutableStateOf("500") }
    var editableAmount by remember { mutableStateOf("0") }
    var isSavingJobReview by remember { mutableStateOf(false) }

    // BackHandler: User can go back, timer will keep running in background!
    BackHandler { onFinish() }

    // Ensure services are seeded
    LaunchedEffect(currentUserId) {
        repository.seedDefaultServicesIfNeeded(currentUserId)
    }

    // Auto-select first service if none selected
    LaunchedEffect(servicesState.value) {
        if (selectedService == null && servicesState.value.isNotEmpty()) {
            val first = servicesState.value.first()
            selectedService = first
            customRate = first.hourlyRate.toInt().toString()
        }
    }

    val isTimerRunning = activeTimerData.isRunning
    val currentRate = if (isTimerRunning) activeTimerData.hourlyRate else (customRate.toDoubleOrNull() ?: selectedService?.hourlyRate ?: 500.0)
    val billableSeconds = if (isTimerRunning) liveElapsedSeconds else 0L
    val billableHours = billableSeconds / 3600.0
    val currentAmount = round(billableHours * currentRate)

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val currentDate = dateFormat.format(Date())
    val startTimeFormatted = if (activeTimerData.initialStartMs > 0L) timeFormat.format(Date(activeTimerData.initialStartMs)) else timeFormat.format(Date())

    val matchingCustomers = customersState.value.filter {
        customerSearchQuery.isBlank() ||
                it.name.contains(customerSearchQuery, ignoreCase = true) ||
                it.mobile.contains(customerSearchQuery) ||
                it.village.contains(customerSearchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            TopAppBar(
                title = { Text("Work Timer & Billing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp + padding.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            if (!isTimerRunning) {
                // Setup and Start Screen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp, pressedElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Customer & Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Searchable Customer Dropdown + Quick Add
                        var customerExpanded by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = customerExpanded,
                                onExpandedChange = { customerExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = customerSearchQuery,
                                    onValueChange = {
                                        customerSearchQuery = it
                                        customerExpanded = true
                                    },
                                    label = { Text("Customer *", maxLines = 1) },
                                    placeholder = { Text("Select / Type Name", maxLines = 1) },
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = customerExpanded,
                                    onDismissRequest = { customerExpanded = false }
                                ) {
                                    if (matchingCustomers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No matching customers") },
                                            onClick = { }
                                        )
                                    } else {
                                        matchingCustomers.forEach { customer ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(customer.name, fontWeight = FontWeight.Bold)
                                                        if (customer.mobile.isNotEmpty() || customer.village.isNotEmpty()) {
                                                            Text(
                                                                "${customer.mobile} ${if (customer.village.isNotEmpty()) "• ${customer.village}" else ""}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
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
                                            newCustomerName = customerSearchQuery
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
                                Icon(Icons.Default.Add, contentDescription = "Add Customer")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Searchable Service Dropdown + Quick Add
                        var serviceExpanded by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = serviceExpanded,
                                onExpandedChange = { serviceExpanded = it },
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
                                                customRate = service.hourlyRate.toInt().toString()
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
                                val customer = selectedCustomer ?: return@Button
                                val rateVal = customRate.toDoubleOrNull() ?: selectedService?.hourlyRate ?: 500.0
                                TimerStateManager.startTimer(
                                    context = context,
                                    customer = customer,
                                    service = selectedService,
                                    rate = rateVal,
                                    notes = notes
                                )
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
                // Active Running Timer Card (Photo 1 - No Edit Rate field during active run)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeTimerData.customer?.name ?: "Customer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeTimerData.service?.name ?: "Working",
                            style = MaterialTheme.typography.bodyMedium
                        )

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
                            text = "Current Amount: ₹${currentAmount.toLong()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Start: $startTimeFormatted", fontWeight = FontWeight.Medium)
                            Text("Rate: ₹${formatIndianCurrency(currentRate)}/hr", fontWeight = FontWeight.Bold)
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
                            if (activeTimerData.isPaused) {
                                TimerStateManager.resumeTimer(context)
                            } else {
                                TimerStateManager.pauseTimer(context)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            if (activeTimerData.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (activeTimerData.isPaused) "Resume" else "Pause", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        editableRate = currentRate.toInt().toString()
                        editableAmount = currentAmount.toLong().toString()
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
        }

        // Quick Add Customer Dialog
        if (showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddCustomerDialog = false
                    customerNameError = null
                    customerMobileError = null
                },
                title = { Text("Add New Customer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = {
                                newCustomerName = it
                                if (customerNameError != null) customerNameError = null
                            },
                            label = { Text("Customer Name *") },
                            singleLine = true,
                            isError = customerNameError != null,
                            supportingText = {
                                customerNameError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        val isMobileError = customerMobileError != null || (newCustomerMobile.isNotEmpty() && newCustomerMobile.length < 10)
                        OutlinedTextField(
                            value = newCustomerMobile,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }.take(10)
                                newCustomerMobile = digitsOnly
                                customerMobileError = if (digitsOnly.isNotEmpty() && digitsOnly.length < 10) {
                                    "Mobile number must be 10 digits (${digitsOnly.length}/10)"
                                } else {
                                    null
                                }
                            },
                            label = { Text("Mobile Number (Optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isMobileError,
                            supportingText = {
                                val err = customerMobileError ?: if (newCustomerMobile.isNotEmpty() && newCustomerMobile.length < 10) {
                                    "Mobile number must be 10 digits (${newCustomerMobile.length}/10)"
                                } else null
                                if (err != null) {
                                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
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
                            val trimmedName = newCustomerName.trim()
                            val trimmedMobile = newCustomerMobile.trim()

                            if (trimmedName.isBlank()) {
                                customerNameError = "Please enter customer name *"
                                return@Button
                            }
                            if (trimmedMobile.isNotEmpty() && trimmedMobile.length < 10) {
                                customerMobileError = "Mobile number must be 10 digits (${trimmedMobile.length}/10)"
                                return@Button
                            }

                            isSavingCustomerDialog = true
                            scope.launch {
                                val existing = customersState.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
                                val newCust = existing?.copy(
                                    mobile = if (trimmedMobile.isNotBlank()) trimmedMobile else existing.mobile,
                                    village = if (newCustomerVillage.isNotBlank()) newCustomerVillage.trim() else existing.village,
                                    updatedAt = System.currentTimeMillis()
                                ) ?: Customer(
                                    customerId = "cust_${System.currentTimeMillis()}",
                                    userId = currentUserId,
                                    name = trimmedName,
                                    mobile = trimmedMobile,
                                    village = newCustomerVillage.trim(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.saveCustomer(newCust)
                                selectedCustomer = newCust
                                customerSearchQuery = newCust.name
                                newCustomerName = ""
                                newCustomerMobile = ""
                                newCustomerVillage = ""
                                customerNameError = null
                                customerMobileError = null
                                isSavingCustomerDialog = false
                                showAddCustomerDialog = false
                            }
                        },
                        enabled = !isSavingCustomerDialog
                    ) {
                        if (isSavingCustomerDialog) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save Customer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddCustomerDialog = false
                        customerNameError = null
                        customerMobileError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Quick Add Service Dialog
        if (showAddServiceDialog) {
            val isServiceNameError = serviceNameError != null && newServiceName.isBlank()
            AlertDialog(
                onDismissRequest = { 
                    showAddServiceDialog = false 
                    serviceNameError = null
                },
                title = { Text("Add New Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newServiceName,
                            onValueChange = { 
                                newServiceName = it
                                serviceNameError = null
                            },
                            label = { Text("Service Name * (e.g. Harrowing)") },
                            singleLine = true,
                            isError = isServiceNameError,
                            supportingText = {
                                if (isServiceNameError) {
                                    Text("Service name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newServiceRate,
                            onValueChange = { newServiceRate = it },
                            label = { Text("Hourly Rate (₹) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newServiceName.isBlank()) {
                                serviceNameError = "Service name is required *"
                                return@Button
                            }
                            isSavingServiceDialog = true
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
                                customRate = sRate.toInt().toString()
                                newServiceName = ""
                                isSavingServiceDialog = false
                                showAddServiceDialog = false
                            }
                        },
                        enabled = !isSavingServiceDialog
                    ) {
                        if (isSavingServiceDialog) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save Service")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddServiceDialog = false 
                        serviceNameError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showReviewDialog) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Review & Confirm Bill", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Customer: ${activeTimerData.customer?.name ?: selectedCustomer?.name}")
                        Text("Service: ${activeTimerData.service?.name ?: selectedService?.name ?: "Service"}")
                        Text("Billable Time: ${billableSeconds / 3600}h ${(billableSeconds % 3600) / 60}m ${billableSeconds % 60}s")

                        OutlinedTextField(
                            value = editableRate,
                            onValueChange = { editableRate = it },
                            label = { Text("Hourly Rate (₹)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editableAmount,
                            onValueChange = { editableAmount = it },
                            label = { Text("Final Amount (₹) [Editable]") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalRate = round(editableRate.toDoubleOrNull() ?: currentRate)
                            val finalAmt = round(editableAmount.toDoubleOrNull() ?: currentAmount)
                            val totalMin = (billableSeconds / 60).toInt()
                            val breakMin = 0
                            val billableMin = maxOf(1, totalMin)

                            val cust = activeTimerData.customer ?: selectedCustomer
                            val srv = activeTimerData.service ?: selectedService
                            val noteText = activeTimerData.notes.ifBlank { notes }

                            isSavingJobReview = true
                            scope.launch {
                                val existingJob = jobsState.value.find { 
                                    it.customerId == (cust?.customerId ?: "") && 
                                    it.serviceId == (srv?.serviceId ?: "") && 
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
                                        paymentStatus = "Pending",
                                        endTime = timeFormat.format(Date()),
                                        notes = if (noteText.isBlank()) existingJob.notes else if (existingJob.notes.isBlank()) noteText else "${existingJob.notes}, $noteText",
                                        createdAt = System.currentTimeMillis()
                                    )
                                } else {
                                    Job(
                                        jobId = "job_${System.currentTimeMillis()}",
                                        userId = currentUserId,
                                        customerId = cust?.customerId ?: "cust_unknown",
                                        customerName = cust?.name ?: "Unknown",
                                        serviceId = srv?.serviceId ?: "srv_unknown",
                                        serviceName = srv?.name ?: "Service",
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
                                        notes = noteText,
                                        createdAt = System.currentTimeMillis()
                                    )
                                }
                                repository.saveJob(job)

                                // Update customer totals
                                cust?.let { c ->
                                    val updatedCust = c.copy(
                                        totalJobs = if (existingJob == null) c.totalJobs + 1 else c.totalJobs,
                                        totalAmount = c.totalAmount + finalAmt,
                                        pendingAmount = c.pendingAmount + finalAmt,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    repository.saveCustomer(updatedCust)
                                }

                                TimerStateManager.stopTimer(context)
                                isSavingJobReview = false
                                showReviewDialog = false
                                onFinish()
                            }
                        },
                        enabled = !isSavingJobReview
                    ) {
                        if (isSavingJobReview) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
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
