package com.example.ui.jobs

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen

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
        "Paid" -> jobsState.value.filter { it.paymentStatus == "Paid" || it.pendingAmount <= 0 }
        "Pending" -> jobsState.value.filter { it.paymentStatus != "Paid" && it.pendingAmount > 0 }
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
                FilterChip(selected = filterStatus == "All", onClick = { filterStatus = "All" }, label = { Text("All (${jobsState.value.size})") })
                FilterChip(
                    selected = filterStatus == "Pending",
                    onClick = { filterStatus = "Pending" },
                    label = { Text("Pending Dues (${jobsState.value.count { it.pendingAmount > 0 }})") }
                )
                FilterChip(
                    selected = filterStatus == "Paid",
                    onClick = { filterStatus = "Paid" },
                    label = { Text("Paid (${jobsState.value.count { it.paymentStatus == "Paid" || it.pendingAmount <= 0 }})") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No jobs found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val isPaid = job.paymentStatus == "Paid" || job.pendingAmount <= 0
    val cardShape = RoundedCornerShape(14.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${job.serviceName} • ${job.date}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${job.finalAmount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (isPaid) "Paid" else "Pending: ₹${job.pendingAmount.toInt()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = if (isPaid) Color(0xFF166534) else Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m", style = MaterialTheme.typography.bodySmall)
                Text("Rate: ₹${job.rate.toInt()}/hr", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
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

    if (job == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isPaid = job.paymentStatus == "Paid" || job.pendingAmount <= 0

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
                        val shareText = "🚜 *TIME BILL INVOICE*\nCustomer: ${job.customerName}\nService: ${job.serviceName}\nDate: ${job.date}\nDuration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m\nAmount: *₹${job.finalAmount.toInt()}*\nStatus: *${if (isPaid) "Paid" else "Pending Due: ₹" + job.pendingAmount.toInt()}*\nThank you for your business!"
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer:", fontWeight = FontWeight.Medium)
                            Text(job.customerName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service:", fontWeight = FontWeight.Medium)
                            Text(job.serviceName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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
                            Text("₹${job.rate.toInt()}/hr")
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("₹${job.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid Amount:", fontWeight = FontWeight.Medium)
                            Text("₹${job.paidAmount.toInt()}", color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Due:", fontWeight = FontWeight.Medium)
                            Text(
                                text = if (job.pendingAmount > 0) "₹${job.pendingAmount.toInt()}" else "₹0 (Fully Paid)",
                                color = if (job.pendingAmount > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
