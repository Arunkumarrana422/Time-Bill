package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.util.formatIndianCurrency
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Job
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    currentUserId: String,
    customerId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val customersState = repository.observeCustomers(currentUserId).collectAsState(initial = emptyList())
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val paymentsState = repository.observePayments(currentUserId).collectAsState(initial = emptyList())

    val customer = customersState.value.find { it.customerId == customerId }
    val customerJobs = jobsState.value.filter { it.customerId == customerId }
    val customerPayments = paymentsState.value.filter { it.customerId == customerId }

    val listState = rememberLazyListState()

    // Hide FAB while scrolling down, show when scrolling up or at top / stopped
    val isFabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.isScrollInProgress || listState.lastScrolledBackward
        }
    }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalBilling = if (customerJobs.isNotEmpty()) customerJobs.sumOf { it.finalAmount } else customer.totalAmount
    val paymentsSum = customerPayments.sumOf { it.amount }
    val jobsPaidSum = customerJobs.sumOf { it.paidAmount }
    
    // Accurate single-counted total paid from recorded payments
    val effectivePaid = if (customerPayments.isNotEmpty()) {
        paymentsSum
    } else if (jobsPaidSum > 0) {
        jobsPaidSum
    } else {
        customer.paidAmount
    }
    
    val pendingDue = maxOf(0.0, totalBilling - effectivePaid)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                    }
                    IconButton(onClick = {
                        val msg = "Hello ${customer.name}, your total bill is ₹${formatIndianCurrency(totalBilling)}, Total Paid: ₹${formatIndianCurrency(effectivePaid)}, and Pending Due: ₹${formatIndianCurrency(pendingDue)} for Time Bill work. Please make payment at your earliest convenience. Thank you!"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${customer.mobile}&text=${Uri.encode(msg)}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Reminder", tint = Color(0xFF25D366))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onNavigate(Screen.ReceivePayment.createRoute(customer.customerId))
                    },
                    icon = { Icon(Icons.Default.Payment, contentDescription = null) },
                    text = { Text("Receive Payment", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        try {
                            repository.syncDataFromFirestore(currentUserId)
                        } catch (e: Exception) {
                            // ignore
                        }
                        delay(650)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp + padding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Customer Ledger Summary Card
            item {
                val summaryCardShape = RoundedCornerShape(16.dp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = summaryCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ledger Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Mobile: ${customer.mobile}")
                        }
                        if (customer.village.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Village / Location: ${customer.village}")
                            }
                        }
                        if (customer.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Notes: ${customer.notes}")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Billing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${formatIndianCurrency(totalBilling)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Paid", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A))
                                Text("₹${formatIndianCurrency(effectivePaid)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16A34A))
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                val isNewCustomer = customerJobs.isEmpty()
                                val pendingColor = if (pendingDue > 0) Color(0xFFDC2626) else if (isNewCustomer) Color(0xFF2563EB) else Color(0xFF16A34A)
                                val pendingText = if (pendingDue > 0) "₹${formatIndianCurrency(pendingDue)}" else if (isNewCustomer) "₹0" else "All Paid"

                                Text("Pending Due", style = MaterialTheme.typography.bodySmall, color = pendingColor)
                                Text(
                                    text = pendingText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = pendingColor
                                )
                            }
                        }
                    }
                }
            }

            // Job History Section
            item {
                Text("Job History (${customerJobs.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (customerJobs.isEmpty()) {
                item {
                    Text("No jobs found for this customer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(customerJobs) { job ->
                    // Calculate accurate job paid & pending dynamically
                    val jobSpecificPayments = customerPayments.filter { it.jobId == job.jobId }.sumOf { it.amount }
                    val actualJobPaid = if (jobSpecificPayments > 0) {
                        jobSpecificPayments
                    } else if (customerJobs.size == 1) {
                        effectivePaid
                    } else {
                        job.paidAmount
                    }
                    val actualJobPending = maxOf(0.0, job.finalAmount - actualJobPaid)
                    val isJobFullyPaid = actualJobPending <= 0.0

                    val jobItemShape = RoundedCornerShape(14.dp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = jobItemShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${job.date} • ${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!isJobFullyPaid && actualJobPaid > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Paid: ₹${formatIndianCurrency(actualJobPaid)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${formatIndianCurrency(job.finalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isJobFullyPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (isJobFullyPaid) "Paid" else "Pending: ₹${formatIndianCurrency(actualJobPending)}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        color = if (isJobFullyPaid) Color(0xFF166534) else Color(0xFF991B1B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment History Section
            item {
                Text("Payment History (${customerPayments.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (customerPayments.isEmpty()) {
                item {
                    Text("No payments recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(customerPayments) { payment ->
                    val paymentCardShape = RoundedCornerShape(14.dp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = paymentCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (payment.notes.isNotBlank() && !payment.notes.contains("job_")) payment.notes else "Payment via ${payment.method}", fontWeight = FontWeight.Bold)
                                Text("${payment.method} • ${payment.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("₹${formatIndianCurrency(payment.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        }
        }
    }
}
