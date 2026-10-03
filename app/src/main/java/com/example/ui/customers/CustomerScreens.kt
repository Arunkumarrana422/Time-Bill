package com.example.ui.customers

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
import com.example.data.model.Customer
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
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
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
                        CustomerCard(
                            customer = customer,
                            onClick = { onNavigate(Screen.CustomerDetail.createRoute(customer.customerId)) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Customer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, singleLine = true)
                        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") }, singleLine = true)
                        OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village / Location") }, singleLine = true)
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isBlank() || mobile.isBlank()) return@Button
                            scope.launch {
                                val customerId = "cust_${System.currentTimeMillis()}"
                                val customer = Customer(
                                    customerId = customerId,
                                    userId = currentUserId,
                                    name = name,
                                    mobile = mobile,
                                    address = address,
                                    village = village,
                                    notes = notes
                                )
                                repository.saveCustomer(customer)
                                name = ""
                                mobile = ""
                                address = ""
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
fun CustomerCard(customer: Customer, onClick: () -> Unit) {
    val context = LocalContext.current
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
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("📞 ${customer.mobile} ${if (customer.village.isNotEmpty()) "• 📍 ${customer.village}" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${customer.mobile}&text=Hello%20${customer.name},%20you%20have%20a%20pending%20balance%20of%20₹${customer.pendingAmount}%20for%20Time%20Bill%20services."))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Jobs: ${customer.totalJobs}", style = MaterialTheme.typography.bodySmall)
                Text("Total: ₹${customer.totalAmount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("Pending: ₹${customer.pendingAmount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (customer.pendingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
