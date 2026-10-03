package com.example.ui.jobs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    var filterStatus by remember { mutableStateOf("All") }

    val filteredJobs = when (filterStatus) {
        "Paid" -> jobsState.value.filter { it.paymentStatus == "Paid" }
        "Pending" -> jobsState.value.filter { it.paymentStatus == "Pending" || it.paymentStatus == "Partially Paid" }
        else -> jobsState.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Job & Invoice Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = false,
                    onClick = { onNavigate(Screen.Dashboard.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Work, contentDescription = null) },
                    label = { Text("Jobs") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Customers") },
                    selected = false,
                    onClick = { onNavigate(Screen.Customers.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Reports") },
                    selected = false,
                    onClick = { onNavigate(Screen.Reports.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { onNavigate(Screen.Settings.route) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filterStatus == "All", onClick = { filterStatus = "All" }, label = { Text("All") })
                FilterChip(selected = filterStatus == "Pending", onClick = { filterStatus = "Pending" }, label = { Text("Pending Dues") })
                FilterChip(selected = filterStatus == "Paid", onClick = { filterStatus = "Paid" }, label = { Text("Paid") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No jobs found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredJobs) { job ->
                        JobCard(job = job, onClick = { onNavigate(Screen.JobDetail.createRoute(job.jobId)) })
                    }
                }
            }
        }
    }
}

@Composable
fun JobCard(job: Job, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(job.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${job.serviceName} • ${job.date}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${job.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (job.paymentStatus) {
                            "Paid" -> MaterialTheme.colorScheme.primaryContainer
                            "Partially Paid" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Text(job.paymentStatus, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Duration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m | Rate: ₹${job.rate.toInt()}/hr", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    currentUserId: String,
    jobId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val job = jobsState.value.find { h -> h.jobId == jobId }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember { mutableStateOf(job?.pendingAmount?.toString() ?: "0") }
    var paymentMethod by remember { mutableStateOf("Cash") }

    val scope = rememberCoroutineScope()

    if (job == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📄 Invoice #${job.jobId.takeLast(6)}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "🚜 *TIME BILL INVOICE*\nCustomer: ${job.customerName}\nService: ${job.serviceName}\nDate: ${job.date}\nDuration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m\nAmount: *₹${job.finalAmount}*\nStatus: *${job.paymentStatus}*\nThank you for your business!"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Invoice"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Invoice")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer:", fontWeight = FontWeight.Medium)
                            Text(job.customerName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service:", fontWeight = FontWeight.Medium)
                            Text(job.serviceName)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date & Time:", fontWeight = FontWeight.Medium)
                            Text("${job.date} (${job.startTime} - ${job.endTime})")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Working Duration:", fontWeight = FontWeight.Medium)
                            Text("${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hourly Rate:", fontWeight = FontWeight.Medium)
                            Text("₹${job.rate}/hr")
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("₹${job.finalAmount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid Amount:", fontWeight = FontWeight.Medium)
                            Text("₹${job.paidAmount}", color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Due:", fontWeight = FontWeight.Medium)
                            Text("₹${job.pendingAmount}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }

                        if (job.pendingAmount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Record Payment for this Job")
                            }
                        }
                    }
                }
            }
        }

        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Record Payment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = paymentAmount,
                            onValueChange = { paymentAmount = it },
                            label = { Text("Amount (₹)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { paymentMethod = it },
                            label = { Text("Method (Cash, UPI, Bank)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = paymentAmount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) return@Button
                            scope.launch {
                                val paymentId = "pay_${System.currentTimeMillis()}"
                                val payment = Payment(
                                    paymentId = paymentId,
                                    userId = currentUserId,
                                    customerId = job.customerId,
                                    customerName = job.customerName,
                                    jobId = job.jobId,
                                    amount = amt,
                                    method = paymentMethod,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    notes = "Payment for job ${job.jobId}"
                                )
                                repository.savePayment(payment)

                                val newPaid = job.paidAmount + amt
                                val newPending = maxOf(0.0, job.finalAmount - newPaid)
                                val newStatus = if (newPending <= 0) "Paid" else "Partially Paid"

                                val updatedJob = job.copy(
                                    paidAmount = newPaid,
                                    pendingAmount = newPending,
                                    paymentStatus = newStatus
                                )
                                repository.saveJob(updatedJob)
                                showPaymentDialog = false
                            }
                        }
                    ) {
                        Text("Save Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
