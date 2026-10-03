package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            }
        }
    }
}
