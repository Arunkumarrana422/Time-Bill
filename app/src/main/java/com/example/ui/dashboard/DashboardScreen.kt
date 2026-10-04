package com.example.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.model.Job
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    showBottomBar: Boolean = true,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }
    var showExitBanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showBottomBar) {
        BackHandler {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedTime = currentTime
                showExitBanner = true
                scope.launch {
                    delay(2000L)
                    showExitBanner = false
                }
            }
        }
    }

    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    val activeTimerData by com.example.service.TimerStateManager.timerData.collectAsState()
    val liveTimerSeconds by com.example.service.TimerStateManager.elapsedSeconds.collectAsState()

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

    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(600)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Time Bill Dashboard", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
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
                            value = "₹${todayEarnings.toInt()}",
                            subtitle = todayHoursFormatted,
                            icon = Icons.Default.TrendingUp,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Pending Payments",
                            value = "₹${totalPending.toInt()}",
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
                            value = "₹${monthEarnings.toInt()}",
                            subtitle = "${monthJobs.size} jobs done",
                            icon = Icons.Default.CalendarMonth,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Received",
                            value = "₹${totalReceived.toInt()}",
                            subtitle = "${paymentsState.value.size} payments",
                            icon = Icons.Default.CheckCircle,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                }

                // Live Active Timer Banner
                if (activeTimerData.isRunning) {
                    item {
                        val hours = liveTimerSeconds / 3600
                        val minutes = (liveTimerSeconds % 3600) / 60
                        val seconds = liveTimerSeconds % 60
                        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                        val liveAmount = ((liveTimerSeconds / 3600.0) * activeTimerData.hourlyRate).toInt()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(Screen.Timer.route) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timelapse,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "⏱️ ${activeTimerData.customer?.name ?: "Customer"} ($timeFormatted)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "${activeTimerData.service?.name ?: "Work"} • ₹$liveAmount (${if (activeTimerData.isPaused) "Paused" else "Running"})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onNavigate(Screen.Timer.route) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Open", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
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
                        QuickActionButton(
                            if (activeTimerData.isRunning) "Live Timer ⏱️" else "Start Timer",
                            if (activeTimerData.isRunning) Icons.Default.Timelapse else Icons.Default.Timer
                        ) { onNavigate(Screen.Timer.route) }
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
                        val emptyCardShape = RoundedCornerShape(14.dp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(emptyCardShape),
                            shape = emptyCardShape
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No jobs recorded yet.", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onNavigate(Screen.Timer.route) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Start Your First Job")
                                }
                            }
                        }
                    }
                } else {
                    items(jobsState.value.take(5)) { job ->
                        val jobCardShape = RoundedCornerShape(14.dp)
                        Card(
                            onClick = { onNavigate(Screen.JobDetail.createRoute(job.jobId)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(jobCardShape),
                            shape = jobCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
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
                                    Text("₹${job.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    val isPaid = job.pendingAmount <= 0.0
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = if (isPaid) "✓ Paid" else "Pending: ₹${job.pendingAmount.toInt()}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            color = if (isPaid) Color(0xFF166534) else Color(0xFF991B1B),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Animated Pill Banner
        AnimatedVisibility(
            visible = showExitBanner,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Press back again to exit",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
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
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier.clip(cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
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
    val buttonShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(buttonShape)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = buttonShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
