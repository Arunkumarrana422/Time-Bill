package com.example.ui.services

import androidx.activity.compose.BackHandler
import com.example.ui.util.formatIndianCurrency
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceItem
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val servicesState = repository.observeServices(currentUserId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("500") }
    var addServiceError by remember { mutableStateOf<String?>(null) }
    var isAddingService by remember { mutableStateOf(false) }

    // Edit service state
    var editingService by remember { mutableStateOf<ServiceItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editHourlyRate by remember { mutableStateOf("") }
    var editServiceError by remember { mutableStateOf<String?>(null) }
    var isUpdatingService by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            TopAppBar(
                title = { Text("Services & Rates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service")
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        try {
                            repository.syncDataFromFirestore(currentUserId)
                        } catch (e: Exception) {
                            // ignore
                        }
                        delay(650)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp + padding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(servicesState.value) { service ->
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val cardShape = RoundedCornerShape(14.dp)
                Card(
                    onClick = {
                        editingService = service
                        editName = service.name
                        editDescription = service.description
                        editHourlyRate = service.hourlyRate.toInt().toString()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (service.description.isNotEmpty()) {
                                Text(service.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("₹${formatIndianCurrency(service.hourlyRate)}/hr", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
                }
            }
        }
        }

        if (showAddDialog) {
            val isNameError = addServiceError != null && name.isBlank()
            val rateVal = hourlyRate.toDoubleOrNull() ?: 0.0
            val isRateError = addServiceError != null && rateVal <= 0.0

            AlertDialog(
                onDismissRequest = { 
                    showAddDialog = false 
                    addServiceError = null
                },
                title = { Text("Add Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                addServiceError = null
                            },
                            label = { Text("Service Name *") },
                            singleLine = true,
                            isError = isNameError,
                            supportingText = {
                                if (isNameError) {
                                    Text("Service name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hourlyRate,
                            onValueChange = { 
                                hourlyRate = it
                                addServiceError = null
                            },
                            label = { Text("Hourly Rate (₹) *") },
                            singleLine = true,
                            isError = isRateError,
                            supportingText = {
                                if (isRateError) {
                                    Text("Valid rate greater than ₹0 is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val hr = hourlyRate.toDoubleOrNull() ?: 0.0
                            if (name.isBlank()) {
                                addServiceError = "Service name is required *"
                                return@Button
                            }
                            if (hr <= 0.0) {
                                addServiceError = "Valid rate is required *"
                                return@Button
                            }
                            isAddingService = true
                            scope.launch {
                                val srvId = "srv_${System.currentTimeMillis()}"
                                val service = ServiceItem(
                                    serviceId = srvId,
                                    userId = currentUserId,
                                    name = name.trim(),
                                    description = description.trim(),
                                    hourlyRate = hr,
                                    minuteRate = hr / 60.0,
                                    isActive = true
                                )
                                repository.saveService(service)
                                name = ""
                                description = ""
                                addServiceError = null
                                isAddingService = false
                                showAddDialog = false
                            }
                        },
                        enabled = !isAddingService
                    ) {
                        if (isAddingService) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddDialog = false 
                        addServiceError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (editingService != null) {
            val isEditNameError = editServiceError != null && editName.isBlank()
            val editRateVal = editHourlyRate.toDoubleOrNull() ?: 0.0
            val isEditRateError = editServiceError != null && editRateVal <= 0.0

            AlertDialog(
                onDismissRequest = { 
                    editingService = null 
                    editServiceError = null
                },
                title = { Text("Edit Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { 
                                editName = it
                                editServiceError = null
                            },
                            label = { Text("Service Name *") },
                            singleLine = true,
                            isError = isEditNameError,
                            supportingText = {
                                if (isEditNameError) {
                                    Text("Service name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editHourlyRate,
                            onValueChange = { 
                                editHourlyRate = it
                                editServiceError = null
                            },
                            label = { Text("Hourly Rate (₹) *") },
                            singleLine = true,
                            isError = isEditRateError,
                            supportingText = {
                                if (isEditRateError) {
                                    Text("Valid rate greater than ₹0 is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val hr = editHourlyRate.toDoubleOrNull() ?: 0.0
                            if (editName.isBlank()) {
                                editServiceError = "Service name is required *"
                                return@Button
                            }
                            if (hr <= 0.0) {
                                editServiceError = "Valid rate is required *"
                                return@Button
                            }
                            isUpdatingService = true
                            scope.launch {
                                val updated = editingService!!.copy(
                                    name = editName.trim(),
                                    description = editDescription.trim(),
                                    hourlyRate = hr,
                                    minuteRate = hr / 60.0
                                )
                                repository.saveService(updated)
                                isUpdatingService = false
                                editServiceError = null
                                editingService = null
                            }
                        },
                        enabled = !isUpdatingService
                    ) {
                        if (isUpdatingService) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        editingService = null 
                        editServiceError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
