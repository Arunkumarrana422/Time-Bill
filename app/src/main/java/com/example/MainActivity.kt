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
import com.example.ui.theme.TimeBillTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp init error: ${e.message}")
        }

        enableEdgeToEdge()

        val repository = TimeBillRepository(this)

        setContent {
            TimeBillTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentUser by remember {
                        mutableStateOf(
                            try {
                                Firebase.auth.currentUser
                            } catch (e: Exception) {
                                null
                            }
                        )
                    }
                    var isGuestMode by remember { mutableStateOf(false) }
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

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

                    val effectiveUserId = currentUser?.uid ?: if (isGuestMode) "offline_user" else null

                    LaunchedEffect(effectiveUserId) {
                        effectiveUserId?.let { uid ->
                            scope.launch {
                                try {
                                    repository.seedDefaultServicesIfNeeded(uid)
                                    repository.observeUserProfile(uid).collectLatest { profile ->
                                        userProfile = profile
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Data load exception: ${e.message}")
                                }
                            }
                        }
                    }

                    if (effectiveUserId == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }
                                if (currentUser == null) {
                                    isGuestMode = true
                                }
                            },
                            onContinueOffline = {
                                isGuestMode = true
                            }
                        )
                    } else {
                        TimeBillNavGraph(
                            navController = navController,
                            repository = repository,
                            currentUserId = effectiveUserId,
                            userProfile = userProfile,
                            onSignOut = {
                                try {
                                    Firebase.auth.signOut()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                currentUser = null
                                isGuestMode = false
                            }
                        )
                    }
                }
            }
        }
    }
}
