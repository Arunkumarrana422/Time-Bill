package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.Customer
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    showBottomBar: Boolean = true,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomerIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedCustomerIds.isNotEmpty()

    BackHandler {
        if (isSelectionMode) {
            selectedCustomerIds = emptySet()
        } else {
            onBack()
        }
    }

    val listState = rememberLazyListState()

    // Hide FAB while scrolling down, show when scrolling up or at top / stopped
    val isFabVisible by remember {
        derivedStateOf {
            !isSelectionMode && (listState.firstVisibleItemIndex == 0 || !listState.isScrollInProgress || listState.lastScrolledBackward)
        }
    }

    val filteredCustomers = customersState.value.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.mobile.contains(searchQuery, ignoreCase = true) ||
        it.village.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedCustomerIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedCustomerIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedCustomerIds.size == filteredCustomers.size) {
                                selectedCustomerIds = emptySet()
                            } else {
                                selectedCustomerIds = filteredCustomers.map { it.customerId }.toSet()
                            }
                        }) {
                            Icon(
                                if (selectedCustomerIds.size == filteredCustomers.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.errorContainer
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            } else {
                TopAppBar(
                    title = { Text("Customers & Ledgers", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
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
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigate(Screen.AddCustomer.route) },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Add Customer", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customer, mobile, village...", maxLines = 1) },
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
            }

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No customers found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers, key = { it.customerId }) { customer ->
                        val customerJobs = jobsState.value.filter { it.customerId == customer.customerId }
                        val customerPayments = paymentsState.value.filter { it.customerId == customer.customerId }
                        
                        val totalBilled = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else customer.totalAmount
                        val paymentsSum = customerPayments.sumOf { it.amount }
                        val jobsPaidSum = customerJobs.sumOf { it.paidAmount }

                        // Single-counted accurate paid amount
                        val effectivePaid = if (customerPayments.isNotEmpty()) {
                            paymentsSum
                        } else if (jobsPaidSum > 0) {
                            jobsPaidSum
                        } else {
                            customer.paidAmount
                        }
                        
                        val pendingDue = maxOf(0.0, totalBilled - effectivePaid)
                        val isSelected = customer.customerId in selectedCustomerIds

                        CustomerCard(
                            customer = customer,
                            totalJobsCount = maxOf(customerJobs.size, customer.totalJobs),
                            totalBilled = totalBilled,
                            totalPaid = effectivePaid,
                            pendingDue = pendingDue,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedCustomerIds = if (isSelected) {
                                    selectedCustomerIds - customer.customerId
                                } else {
                                    selectedCustomerIds + customer.customerId
                                }
                            },
                            onLongClick = {
                                selectedCustomerIds = selectedCustomerIds + customer.customerId
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedCustomerIds = if (isSelected) {
                                        selectedCustomerIds - customer.customerId
                                    } else {
                                        selectedCustomerIds + customer.customerId
                                    }
                                } else {
                                    onNavigate(Screen.CustomerDetail.createRoute(customer.customerId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        val count = selectedCustomerIds.size
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = if (count > 1) "Delete $count Customers?" else "Delete Customer?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (count > 1)
                        "Are you sure you want to delete these $count customers? Their customer profiles will be removed from your list. Note: Previous work records and reports will remain safe."
                    else
                        "Are you sure you want to delete this customer? The profile will be removed from your customer list. Note: Previous work records and reports will remain safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = customersState.value.filter { it.customerId in selectedCustomerIds }
                        scope.launch {
                            toDelete.forEach { customer ->
                                repository.deleteCustomer(customer)
                            }
                            selectedCustomerIds = emptySet()
                            showDeleteDialog = false
                            Toast.makeText(context, "$count customer(s) deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerCard(
    customer: Customer,
    totalJobsCount: Int,
    totalBilled: Double,
    totalPaid: Double,
    pendingDue: Double,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 4.dp else 2.5.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = customer.mobile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (customer.village.isNotEmpty()) {
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = customer.village,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isSelectionMode) {
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
