package com.example.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TimeBillRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onBack: () -> Unit
) {
    val jobsState = repository.observeJobs(currentUserId).collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    val jobsForDate = jobsState.value.filter { it.date == selectedDate }
    val dayEarnings = jobsForDate.sumOf { it.finalAmount }
    val dayMinutes = jobsForDate.sumOf { it.billableDurationMinutes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Job Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Select Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            val summaryCardShape = RoundedCornerShape(14.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(summaryCardShape),
                shape = summaryCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Date: $selectedDate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Jobs: ${jobsForDate.size} • Hours: ${dayMinutes / 60}h ${dayMinutes % 60}m • Earnings: ₹${dayEarnings.toInt()}")
                }
            }

            Text("Jobs on this date:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(jobsForDate) { job ->
                    val jobCardShape = RoundedCornerShape(12.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(jobCardShape),
                        shape = jobCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(job.customerName, fontWeight = FontWeight.Bold)
                                Text("${job.serviceName} (${job.billableDurationMinutes / 60}h)", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("₹${job.finalAmount.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
