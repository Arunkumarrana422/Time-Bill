package com.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.TimeBillRepository
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.navigation.TimeBillNavGraph
import com.example.ui.util.clearFocusOnTap
import com.example.ui.util.isInitialNetworkConnected
import com.example.ui.util.observeNetworkConnectivity
import com.example.ui.theme.TimeBillTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:648141005997:android:31dcf5a9729b4979c65224")
                .setApiKey("AIzaSyAUe5cJqA1PDO6LLe0a4Hv1vdDjuM8WEuk")
                .setProjectId("time-bill-management")
                .setDatabaseUrl("https://time-bill-management-default-rtdb.firebaseio.com")
                .setStorageBucket("time-bill-management.firebasestorage.app")
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp init error: ${e.message}")
        }

        enableEdgeToEdge()

        val repository = TimeBillRepository(this)

        setContent {
            TimeBillTheme {
                val context = LocalContext.current
                val isConnectedState = observeNetworkConnectivity(context).collectAsState(initial = remember { isInitialNetworkConnected(context) })
                val isConnected = isConnectedState.value

                var currentUser by remember {
                    mutableStateOf(
                        try {
                            Firebase.auth.currentUser
                        } catch (e: Exception) {
                            null
                        }
                    )
                }
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                var userProfile by remember { mutableStateOf<UserProfile?>(null) }

                val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                var isAppResumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }

                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        isAppResumed = (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME)
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Show offline toast at most once only when active in foreground, never loop or repeat when minimized
                var previousConnected by remember { mutableStateOf<Boolean?>(null) }
                var offlineToastShown by remember { mutableStateOf(false) }

                LaunchedEffect(isConnected, isAppResumed) {
                    val isActivelyForeground = isAppResumed && lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                    if (isConnected) {
                        offlineToastShown = false
                        if (previousConnected == false) {
                            currentUser?.uid?.let { uid ->
                                scope.launch {
                                    try {
                                        repository.syncDataFromFirestore(uid)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }
                        }
                    } else {
                        // Offline
                        if (isActivelyForeground && previousConnected == true && !offlineToastShown) {
                            try {
                                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                                offlineToastShown = true
                            } catch (e: Exception) {}
                        }
                    }
                    previousConnected = isConnected
                }

                DisposableEffect(Unit) {
                    val auth = try { Firebase.auth } catch (e: Exception) { null }
                    val listener = FirebaseAuth.AuthStateListener { fAuth ->
                        currentUser = fAuth.currentUser
                    }
                    auth?.addAuthStateListener(listener)
                    onDispose {
                        try {
                            auth?.removeAuthStateListener(listener)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        userProfile = null
                    } else {
                        currentUser?.uid?.let { uid ->
                            scope.launch {
                                try {
                                    repository.startRealtimeProfileListener(uid, scope)
                                    repository.syncDataFromFirestore(uid)
                                    repository.seedDefaultServicesIfNeeded(uid)
                                    val profile = repository.getUser(uid)
                                    userProfile = profile

                                    repository.observeUserProfile(uid).collectLatest { p ->
                                        userProfile = p
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Data load exception: ${e.message}")
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnTap()
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (currentUser == null) {
                            AuthScreen(
                                repository = repository,
                                onAuthSuccess = {
                                    currentUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }
                                }
                            )
                        } else {
                            val userId = currentUser!!.uid
                            TimeBillNavGraph(
                                navController = navController,
                                repository = repository,
                                currentUserId = userId,
                                userProfile = userProfile,
                                onSignOut = {
                                    try {
                                        Firebase.auth.signOut()
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    currentUser = null
                                }
                            )
                        }
                    }

                    // Blocking Offline Overlay when no internet
                    if (!isConnected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                .zIndex(9999f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "No Internet",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "No Internet Connection",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Application activity and data loading are paused because there is no active internet connection. The warning toast will remain until connection is re-established.\n\nOnce internet is found, the app will resume automatically.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
