package com.example.ui.expenses

import androidx.activity.compose.BackHandler
import com.example.ui.util.formatIndianCurrency
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val expensesState = repository.observeExpenses(currentUserId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fuel / Diesel") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expenseError by remember { mutableStateOf<String?>(null) }
    var isSavingExpense by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            TopAppBar(
                title = { Text("Expenses Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(expensesState.value) { expense ->
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val expenseCardShape = RoundedCornerShape(14.dp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = expenseCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp, pressedElevation = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(expense.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Category: ${expense.category} • ${expense.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (expense.description.isNotEmpty()) {
                                Text(expense.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("-₹${formatIndianCurrency(expense.amount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            }
        }
        }

        if (showAddDialog) {
            val isNameError = expenseError != null && name.isBlank()
            val amtVal = amount.toDoubleOrNull() ?: 0.0
            val isAmountError = expenseError != null && amtVal <= 0.0

            AlertDialog(
                onDismissRequest = { 
                    showAddDialog = false 
                    expenseError = null
                },
                title = { Text("Add Business Expense", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                expenseError = null
                            },
                            label = { Text("Expense Name * (e.g. Diesel)") },
                            singleLine = true,
                            isError = isNameError,
                            supportingText = {
                                if (isNameError) {
                                    Text("Expense name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (Fuel, Repair, Maintenance)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { 
                                amount = it
                                expenseError = null
                            },
                            label = { Text("Amount (₹) *") },
                            singleLine = true,
                            isError = isAmountError,
                            supportingText = {
                                if (isAmountError) {
                                    Text("Valid amount greater than ₹0 is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = round(amount.toDoubleOrNull() ?: 0.0)
                            if (name.isBlank()) {
                                expenseError = "Expense name is required *"
                                return@Button
                            }
                            if (amt <= 0) {
                                expenseError = "Valid amount greater than ₹0 is required *"
                                return@Button
                            }
                            isSavingExpense = true
                            scope.launch {
                                val expId = "exp_${System.currentTimeMillis()}"
                                val expense = Expense(
                                    expenseId = expId,
                                    userId = currentUserId,
                                    name = name.trim(),
                                    category = category.trim(),
                                    amount = amt,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    description = description.trim()
                                )
                                repository.saveExpense(expense)
                                name = ""
                                amount = ""
                                description = ""
                                expenseError = null
                                isSavingExpense = false
                                showAddDialog = false
                            }
                        },
                        enabled = !isSavingExpense
                    ) {
                        if (isSavingExpense) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save Expense")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddDialog = false 
                        expenseError = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
