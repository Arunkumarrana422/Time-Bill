package com.example.ui.jobs

import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    showBottomBar: Boolean = true,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    var filterStatus by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val allJobs = jobsState.value
    val pendingJobs = remember(allJobs) { allJobs.filter { it.pendingAmount > 0 } }
    val paidJobs = remember(allJobs) { allJobs.filter { it.pendingAmount <= 0 } }

    val baseJobs = when (filterStatus) {
        "Pending" -> pendingJobs
        "Paid" -> paidJobs
        else -> allJobs
    }

    val filteredJobs = if (searchQuery.isBlank()) {
        baseJobs
    } else {
        baseJobs.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
            it.serviceName.contains(searchQuery, ignoreCase = true) ||
            it.date.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job & Invoice Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search jobs, customer...", maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Consistent Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == "All",
                    onClick = { filterStatus = "All" },
                    label = { Text("All (${allJobs.size})", fontWeight = if (filterStatus == "All") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = filterStatus == "Pending",
                    onClick = { filterStatus = "Pending" },
                    label = { Text("Pending Dues (${pendingJobs.size})", fontWeight = if (filterStatus == "Pending") FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = filterStatus == "Paid",
                    onClick = { filterStatus = "Paid" },
                    label = { Text("Paid (${paidJobs.size})", fontWeight = if (filterStatus == "Paid") FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredJobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (filterStatus == "Pending") "No pending dues found! All jobs are paid." else "No jobs found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    val isPaid = job.pendingAmount <= 0
    val cardShape = RoundedCornerShape(14.dp)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.customerName.ifBlank { "Customer" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = job.serviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = job.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${job.finalAmount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (isPaid) "✓ Paid" else "Pending: ₹${job.pendingAmount.toInt()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            color = if (isPaid) Color(0xFF166534) else Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m (@ ₹${job.rate.toInt()}/hr)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isPaid && job.paidAmount > 0) {
                    Text(
                        text = "Paid: ₹${job.paidAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF16A34A)
                    )
                }
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

    val isPaid = job.pendingAmount <= 0
    val detailCardShape = RoundedCornerShape(16.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "*TIME BILL INVOICE*\nCustomer: ${job.customerName}\nService: ${job.serviceName}\nDate: ${job.date}\nDuration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m\nRate: ₹${job.rate.toInt()}/hr\nTotal Bill: *₹${job.finalAmount.toInt()}*\nTotal Paid: *₹${job.paidAmount.toInt()}*\nStatus: *${if (isPaid) "Paid" else "Pending Due: ₹" + job.pendingAmount.toInt()}*\nThank you for your business!"
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
                    shape = detailCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer Name:", fontWeight = FontWeight.Medium)
                            Text(job.customerName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service Name:", fontWeight = FontWeight.Medium)
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
                            Text("Total Bill Amount:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
