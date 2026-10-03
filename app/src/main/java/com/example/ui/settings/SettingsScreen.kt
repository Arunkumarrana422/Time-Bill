package com.example.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TimeBillRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

import com.example.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings & Account", fontWeight = FontWeight.Bold) },
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
                    selected = false,
                    onClick = { onNavigate(Screen.Reports.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = true,
                    onClick = {}
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("User Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Email: ${auth.currentUser?.email ?: "Offline User"}")
                    Text("User ID: $currentUserId")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cloud Sync & Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Data is automatically synced with Firebase Firestore when online.")
                    Button(
                        onClick = {
                            // Manual sync action
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Now")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Account Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(
                        onClick = {
                            auth.signOut()
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}
