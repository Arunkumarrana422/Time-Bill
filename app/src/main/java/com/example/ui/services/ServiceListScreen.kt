package com.example.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceItem
import com.example.data.repository.TimeBillRepository
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

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("500") }
    var minCharge by remember { mutableStateOf("100") }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Services & Rates", fontWeight = FontWeight.Bold) },
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
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service")
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
            items(servicesState.value) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${service.hourlyRate}/hr", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Min: ₹${service.minimumCharge}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Service") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name *") }, singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true)
                        OutlinedTextField(value = hourlyRate, onValueChange = { hourlyRate = it }, label = { Text("Hourly Rate (₹) *") }, singleLine = true)
                        OutlinedTextField(value = minCharge, onValueChange = { minCharge = it }, label = { Text("Minimum Charge (₹)") }, singleLine = true)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            scope.launch {
                                val srvId = "srv_${System.currentTimeMillis()}"
                                val hr = hourlyRate.toDoubleOrNull() ?: 500.0
                                val service = ServiceItem(
                                    serviceId = srvId,
                                    userId = currentUserId,
                                    name = name,
                                    description = description,
                                    hourlyRate = hr,
                                    minuteRate = hr / 60.0,
                                    minimumCharge = minCharge.toDoubleOrNull() ?: 100.0,
                                    isActive = true
                                )
                                repository.saveService(service)
                                name = ""
                                description = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save Service")
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
