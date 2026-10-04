package com.example.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    showBottomBar: Boolean = true,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())
    val expensesState = repository.observeExpenses(currentUserId).collectAsState(initial = emptyList())
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())

    val totalIncome = jobsState.value.sumOf { it.finalAmount }
    val totalReceived = paymentsState.value.sumOf { it.amount }
    val totalPending = maxOf(0.0, totalIncome - totalReceived)
    val totalExpenses = expensesState.value.sumOf { it.amount }
    val netProfit = totalIncome - totalExpenses
    val totalMinutes = jobsState.value.sumOf { it.billableDurationMinutes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Reports & Analytics", fontWeight = FontWeight.Bold) },
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
                        selected = true,
                        onClick = {}
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Profit Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Business Profit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "₹${netProfit.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Revenue (₹${totalIncome.toInt()}) - Expenses (₹${totalExpenses.toInt()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 2x2 Grid of Colorful Cards
            Text("Financial Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsStatCard(
                    title = "Total Invoiced",
                    amount = "₹${totalIncome.toInt()}",
                    icon = Icons.Default.TrendingUp,
                    cardColor = Color(0xFFE0F2FE),
                    textColor = Color(0xFF0369A1),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Total Received",
                    amount = "₹${totalReceived.toInt()}",
                    icon = Icons.Default.CheckCircle,
                    cardColor = Color(0xFFDCFCE7),
                    textColor = Color(0xFF15803D),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsStatCard(
                    title = "Pending Dues",
                    amount = "₹${totalPending.toInt()}",
                    icon = Icons.Default.HourglassTop,
                    cardColor = Color(0xFFFFEDD5),
                    textColor = Color(0xFFC2410C),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Total Expenses",
                    amount = "₹${totalExpenses.toInt()}",
                    icon = Icons.Default.ReceiptLong,
                    cardColor = Color(0xFFFFE4E6),
                    textColor = Color(0xFFBE123C),
                    modifier = Modifier.weight(1f)
                )
            }

            // Productivity & Work Summary Card
            Text("Work & Operations", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProductivityRow(
                        title = "Total Working Hours",
                        value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF6366F1)
                    )
                    HorizontalDivider()
                    ProductivityRow(
                        title = "Total Jobs Completed",
                        value = "${jobsState.value.size} Jobs",
                        icon = Icons.Default.Agriculture,
                        iconTint = Color(0xFF0284C7)
                    )
                    HorizontalDivider()
                    ProductivityRow(
                        title = "Active Customers",
                        value = "${customersState.value.size} Clients",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF0D9488)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsStatCard(
    title: String,
    amount: String,
    icon: ImageVector,
    cardColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = textColor)
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(amount, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun ProductivityRow(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
