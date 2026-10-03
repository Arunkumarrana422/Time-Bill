package com.example

import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TimeBillRepository(this)

        setContent {
            TimeBillTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            currentUser = auth.currentUser
                        }
                        Firebase.auth.addAuthStateListener(listener)
                        onDispose {
                            Firebase.auth.removeAuthStateListener(listener)
                        }
                    }

                    LaunchedEffect(currentUser) {
                        currentUser?.uid?.let { uid ->
                            scope.launch {
                                repository.seedDefaultServicesIfNeeded(uid)
                                repository.observeUserProfile(uid).collectLatest { profile ->
                                    userProfile = profile
                                }
                            }
                        }
                    }

                    if (currentUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
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
                                currentUser = null
                            }
                        )
                    }
                }
            }
        }
    }
}
