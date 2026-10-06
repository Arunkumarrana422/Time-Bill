package com.example.ui.calendar

import android.app.DatePickerDialog
import com.example.ui.util.formatIndianCurrency
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TimeBillRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CalendarFilterMode {
    TODAY,
    YESTERDAY,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM_DATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthPrefixFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val yearPrefixFormat = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }

    var selectedDate by remember { mutableStateOf(dateFormat.format(Date())) }
    var filterMode by remember { mutableStateOf(CalendarFilterMode.TODAY) }

    val todayStr = remember { dateFormat.format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        dateFormat.format(cal.time)
    }
    val currentMonthPrefix = remember { monthPrefixFormat.format(Date()) }
    val currentMonthDisplay = remember { monthFormat.format(Date()) }
    val currentYearPrefix = remember { yearPrefixFormat.format(Date()) }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(selectedDate)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDate = chosenDate
                filterMode = when (chosenDate) {
                    todayStr -> CalendarFilterMode.TODAY
                    yesterdayStr -> CalendarFilterMode.YESTERDAY
                    else -> CalendarFilterMode.CUSTOM_DATE
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val (filteredJobs, displayPeriodTitle) = remember(filterMode, selectedDate, jobsState.value) {
        when (filterMode) {
            CalendarFilterMode.TODAY -> {
                val jobs = jobsState.value.filter { it.date == todayStr }
                jobs to "Today ($todayStr)"
            }
            CalendarFilterMode.YESTERDAY -> {
                val jobs = jobsState.value.filter { it.date == yesterdayStr }
                jobs to "Yesterday ($yesterdayStr)"
            }
            CalendarFilterMode.THIS_MONTH -> {
                val jobs = jobsState.value.filter { it.date.startsWith(currentMonthPrefix) }
                jobs to "Month: $currentMonthDisplay"
            }
            CalendarFilterMode.THIS_YEAR -> {
                val jobs = jobsState.value.filter { it.date.startsWith(currentYearPrefix) }
                jobs to "Year: $currentYearPrefix"
            }
            CalendarFilterMode.CUSTOM_DATE -> {
                val jobs = jobsState.value.filter { it.date == selectedDate }
                jobs to "Date: $selectedDate"
            }
        }
    }

    val totalEarnings = filteredJobs.sumOf { it.finalAmount }
    val totalMinutes = filteredJobs.sumOf { it.billableDurationMinutes }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            TopAppBar(
                title = { Text("Job Calendar", fontWeight = FontWeight.Bold) },
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Entire input field is clickable to open DatePickerDialog
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker() }
            ) {
                OutlinedTextField(
                    value = if (filterMode == CalendarFilterMode.THIS_MONTH) currentMonthDisplay else if (filterMode == CalendarFilterMode.THIS_YEAR) "Year $currentYearPrefix" else selectedDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Select Date / Calendar") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = "Pick Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Quick Filter Chips: Today, Yesterday, This Month, This Year
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == CalendarFilterMode.TODAY,
                    onClick = {
                        selectedDate = todayStr
                        filterMode = CalendarFilterMode.TODAY
                    },
                    label = { Text("Today") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterMode == CalendarFilterMode.YESTERDAY,
                    onClick = {
                        selectedDate = yesterdayStr
                        filterMode = CalendarFilterMode.YESTERDAY
                    },
                    label = { Text("Yesterday") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterMode == CalendarFilterMode.THIS_MONTH,
                    onClick = {
                        filterMode = CalendarFilterMode.THIS_MONTH
                    },
                    label = { Text("This Month") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterMode == CalendarFilterMode.THIS_YEAR,
                    onClick = {
                        filterMode = CalendarFilterMode.THIS_YEAR
                    },
                    label = { Text("This Year") },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Summary Card
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val summaryCardShape = RoundedCornerShape(14.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = summaryCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 7.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(displayPeriodTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Jobs: ${filteredJobs.size} • Hours: ${totalMinutes / 60}h ${totalMinutes % 60}m • Earnings: ₹${formatIndianCurrency(totalEarnings)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text("Jobs List (${filteredJobs.size}):", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (filteredJobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No jobs recorded for this period.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredJobs) { job ->
                        val jobCardShape = RoundedCornerShape(12.dp)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = jobCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp, pressedElevation = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(job.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${job.serviceName} • ${job.date} (${job.billableDurationMinutes / 60}h ${job.billableDurationMinutes % 60}m)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "₹${formatIndianCurrency(job.finalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
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
    }
}
