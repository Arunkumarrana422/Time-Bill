package com.example.ui.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val todayJobs = jobsState.value.filter { it.date == todayStr }
    val todayMinutes = todayJobs.sumOf { it.billableDurationMinutes }
    val todayEarnings = todayJobs.sumOf { it.finalAmount }
    val todayHoursFormatted = String.format(Locale.getDefault(), "%dh %02dm", todayMinutes / 60, todayMinutes % 60)

    val monthJobs = jobsState.value.filter { it.date.startsWith(currentMonthStr) }
    val monthEarnings = monthJobs.sumOf { it.finalAmount }

    val totalPending = jobsState.value.sumOf { it.pendingAmount }
    val totalReceived = paymentsState.value.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚜 Time Bill Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Work, contentDescription = null) },
                    label = { Text("Jobs") },
                    selected = false,
                    onClick = { onNavigate(Screen.Jobs.route) }
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
                    icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                    label = { Text("More") },
                    selected = false,
                    onClick = { onNavigate(Screen.Settings.route) }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.Timer.route) },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Start Work", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            // Summary Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Today's Earnings",
                        value = "₹$todayEarnings",
                        subtitle = todayHoursFormatted,
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Payments",
                        value = "₹$totalPending",
                        subtitle = "${customersState.value.size} customers",
                        icon = Icons.Default.Pending,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Month Earnings",
                        value = "₹$monthEarnings",
                        subtitle = "${monthJobs.size} jobs done",
                        icon = Icons.Default.CalendarMonth,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Received",
                        value = "₹$totalReceived",
                        subtitle = "${paymentsState.value.size} payments",
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton("Start Timer", Icons.Default.Timer) { onNavigate(Screen.Timer.route) }
                    QuickActionButton("Manual Job", Icons.Default.EditNote) { onNavigate(Screen.ManualJob.route) }
                    QuickActionButton("Customers", Icons.Default.PersonAdd) { onNavigate(Screen.Customers.route) }
                    QuickActionButton("Expenses", Icons.Default.Receipt) { onNavigate(Screen.Expenses.route) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton("Payments", Icons.Default.Payments) { onNavigate(Screen.Payments.route) }
                    QuickActionButton("Calendar", Icons.Default.DateRange) { onNavigate(Screen.Calendar.route) }
                    QuickActionButton("Services", Icons.Default.Build) { onNavigate(Screen.Services.route) }
                    QuickActionButton("Reports", Icons.Default.Assessment) { onNavigate(Screen.Reports.route) }
                }
            }

            // Recent Jobs Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Jobs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate(Screen.Jobs.route) }) {
                        Text("View All")
                    }
                }
            }

            if (jobsState.value.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No jobs recorded yet.", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onNavigate(Screen.Timer.route) }) {
                                Text("Start Your First Job")
                            }
                        }
                    }
                }
            } else {
                items(jobsState.value.take(5)) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.JobDetail.createRoute(job.jobId)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${job.serviceName} • ${job.date}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Duration: ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${job.finalAmount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (job.paymentStatus) {
                                        "Paid" -> MaterialTheme.colorScheme.primaryContainer
                                        "Partially Paid" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                ) {
                                    Text(
                                        text = job.paymentStatus,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RowScope.QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
