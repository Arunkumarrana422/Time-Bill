package com.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.TimeBillRepository
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.components.OfflineBanner
import com.example.ui.navigation.Screen
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

                var isAutoSyncing by remember { mutableStateOf(false) }

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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var offlineTriggerCount by remember { mutableIntStateOf(0) }
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

                var previousConnected by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(isConnected, isAppResumed) {
                    if (isConnected) {
                        if (previousConnected == false) {
                            // Automatically trigger loading and sync when internet is restored
                            isAutoSyncing = true
                            currentUser?.uid?.let { uid ->
                                scope.launch {
                                    try {
                                        repository.syncDataFromFirestore(uid)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    delay(1200)
                                    isAutoSyncing = false
                                }
                            } ?: run {
                                delay(1000)
                                isAutoSyncing = false
                            }
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

                LaunchedEffect(currentUser, isConnected) {
                    if (currentUser == null) {
                        userProfile = null
                    } else {
                        currentUser?.uid?.let { uid ->
                            scope.launch {
                                try {
                                    // 1. Always load local profile from Room Database instantly
                                    val profile = repository.getUser(uid)
                                    userProfile = profile
                                    repository.seedDefaultServicesIfNeeded(uid)

                                    // 2. Only sync from Firestore when internet is available
                                    if (isConnected) {
                                        repository.startRealtimeProfileListener(uid, scope)
                                        repository.syncDataFromFirestore(uid)
                                    }

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
                        .pointerInput(isConnected, currentRoute) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press) {
                                        if (!isConnected && currentRoute != Screen.Timer.route) {
                                            offlineTriggerCount++
                                        }
                                    }
                                }
                            }
                        }
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
                                isConnected = isConnected,
                                onOfflineActionBlocked = {
                                    offlineTriggerCount++
                                },
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

                    // Top Offline Banner matching reference video sample
                    OfflineBanner(
                        isOffline = !isConnected,
                        triggerKey = offlineTriggerCount,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Automatic loading & sync indicator when internet returns
                    AnimatedVisibility(
                        visible = isAutoSyncing,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .zIndex(9999f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Internet restored • Syncing data...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
