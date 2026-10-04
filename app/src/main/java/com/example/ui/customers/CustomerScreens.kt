package com.example.ui.customers

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.Payment
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val filteredCustomers = customersState.value.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.mobile.contains(searchQuery) ||
                it.village.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 Customers (${customersState.value.size})", fontWeight = FontWeight.Bold) },
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
                    selected = true,
                    onClick = {}
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, mobile, village...") },
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No customers found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        val customerJobs = jobsState.value.filter { it.customerId == customer.customerId }
                        val customerPayments = paymentsState.value.filter { it.customerId == customer.customerId }
                        
                        val totalBilled = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else customer.totalAmount
                        val totalPaid = customerPayments.sumOf { it.amount } + customerJobs.sumOf { it.paidAmount }
                        val effectivePaid = maxOf(customer.paidAmount, totalPaid)
                        val pendingDue = maxOf(0.0, totalBilled - effectivePaid)

                        CustomerCard(
                            customer = customer,
                            totalJobsCount = maxOf(customerJobs.size, customer.totalJobs),
                            totalBilled = totalBilled,
                            totalPaid = effectivePaid,
                            pendingDue = pendingDue,
                            onClick = { onNavigate(Screen.CustomerDetail.createRoute(customer.customerId)) }
                        )
                    }
                }
            }
        }

        // Add Customer Dialog without redundant address field
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Customer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer Name *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = village,
                            onValueChange = { village = it },
                            label = { Text("Village / Location") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isBlank() || mobile.isBlank()) return@Button
                            scope.launch {
                                val existing = customersState.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
                                val customer = existing?.copy(
                                    mobile = mobile.trim(),
                                    village = village.trim(),
                                    notes = notes.trim(),
                                    updatedAt = System.currentTimeMillis()
                                ) ?: Customer(
                                    customerId = "cust_${System.currentTimeMillis()}",
                                    userId = currentUserId,
                                    name = name.trim(),
                                    mobile = mobile.trim(),
                                    village = village.trim(),
                                    notes = notes.trim()
                                )
                                repository.saveCustomer(customer)
                                name = ""
                                mobile = ""
                                village = ""
                                notes = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save Customer")
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

@Composable
fun CustomerCard(
    customer: Customer,
    totalJobsCount: Int,
    totalBilled: Double,
    totalPaid: Double,
    pendingDue: Double,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
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
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "📞 ${customer.mobile} ${if (customer.village.isNotEmpty()) "• 📍 ${customer.village}" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            val msg = "Hello ${customer.name}, your total bill is ₹${totalBilled.toInt()}, Total Paid: ₹${totalPaid.toInt()}, and Pending Due: ₹${pendingDue.toInt()} for Time Bill work. Thank you!"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${customer.mobile}&text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
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
                Column {
                    Text("Total Jobs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalJobsCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${totalBilled.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A))
                    Text("₹${totalPaid.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Dues", style = MaterialTheme.typography.labelSmall, color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                    Text(
                        text = if (pendingDue > 0) "₹${pendingDue.toInt()}" else "✓ All Paid",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingDue > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}
