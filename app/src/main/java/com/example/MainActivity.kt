package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.TimeBillRepository
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.navigation.TimeBillNavGraph
import com.example.ui.splash.SplashScreen
import com.example.ui.util.clearFocusOnTap
import com.example.ui.theme.TimeBillTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
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
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnTap()
                ) {
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
                    var isProfileLoaded by remember { mutableStateOf(false) }

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
                            isProfileLoaded = true
                        } else {
                            currentUser?.uid?.let { uid ->
                                scope.launch {
                                    try {
                                        // Silent sync and profile load
                                        repository.syncDataFromFirestore(uid)
                                        repository.seedDefaultServicesIfNeeded(uid)
                                        val profile = repository.getUser(uid)
                                        userProfile = profile
                                        isProfileLoaded = true

                                        repository.observeUserProfile(uid).collectLatest { p ->
                                            userProfile = p
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Data load exception: ${e.message}")
                                        isProfileLoaded = true
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
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

                        // Transparent blur circle loading indicator while data loads on startup
                        if (currentUser != null && !isProfileLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                    shadowElevation = 8.dp
                                ) {
                                    Box(
                                        modifier = Modifier.size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(44.dp),
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            strokeWidth = 4.dp
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
