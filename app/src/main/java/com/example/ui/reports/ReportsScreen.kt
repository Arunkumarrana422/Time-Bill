package com.example.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())
    val expensesState = repository.observeExpenses(currentUserId).collectAsState(initial = emptyList())

    val totalIncome = jobsState.value.sumOf { it.finalAmount }
    val totalReceived = paymentsState.value.sumOf { it.amount }
    val totalPending = jobsState.value.sumOf { it.pendingAmount }
    val totalExpenses = expensesState.value.sumOf { it.amount }
    val netProfit = totalIncome - totalExpenses
    val totalMinutes = jobsState.value.sumOf { it.billableDurationMinutes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Business Reports & Analytics", fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Financial Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Divider()
                    ReportRow("Total Invoiced Revenue", "₹${totalIncome.toInt()}")
                    ReportRow("Total Payments Received", "₹${totalReceived.toInt()}")
                    ReportRow("Total Pending Dues", "₹${totalPending.toInt()}")
                    ReportRow("Total Business Expenses", "₹${totalExpenses.toInt()}")
                    Divider()
                    ReportRow("Net Profit", "₹${netProfit.toInt()}", isBold = true, color = MaterialTheme.colorScheme.primary)
                    ReportRow("Total Working Hours", "${totalMinutes / 60}h ${totalMinutes % 60}m")
                    ReportRow("Total Jobs Completed", "${jobsState.value.size}")
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, isBold: Boolean = false, color: androidx.compose.ui.graphics.Color = LocalContentColor.current) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, fontSize = 16.sp, color = color)
    }
}
