package com.example.ui.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.util.clearFocusOnTap
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    repository: TimeBillRepository? = null,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun getFirebaseAuth(): FirebaseAuth {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:648141005997:android:31dcf5a9729b4979c65224")
                .setApiKey("AIzaSyAUe5cJqA1PDO6LLe0a4Hv1vdDjuM8WEuk")
                .setProjectId("time-bill-management")
                .setDatabaseUrl("https://time-bill-management-default-rtdb.firebaseio.com")
                .setStorageBucket("time-bill-management.firebasestorage.app")
                .build()
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext, options)
            } else {
                FirebaseApp.getInstance()
            }
            FirebaseAuth.getInstance(app)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .clearFocusOnTap(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Branding Header
                Text(
                    text = "Time Bill",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Professional Time Tracking & Billing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                val authCardShape = RoundedCornerShape(20.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(authCardShape),
                    shape = authCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isForgotPassword) {
                            // Top Switch Tabs: Login | Create Account
                            TabRow(
                                selectedTabIndex = if (isRegisterMode) 1 else 0,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                divider = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Tab(
                                    selected = !isRegisterMode,
                                    onClick = {
                                        isRegisterMode = false
                                        errorMessage = null
                                        successMessage = null
                                    },
                                    text = { Text("Login", fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal) }
                                )
                                Tab(
                                    selected = isRegisterMode,
                                    onClick = {
                                        isRegisterMode = true
                                        errorMessage = null
                                        successMessage = null
                                    },
                                    text = { Text("Create Account", fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        } else {
                            // Forgot Password Header
                            Text(
                                text = "Reset Password",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter your registered email to receive reset instructions.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (errorMessage != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (successMessage != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = successMessage!!,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business / Service Name") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isForgotPassword) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (!isRegisterMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            isForgotPassword = true
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Forgot Password?",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else if (!isForgotPassword) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Main Action Button
                        Button(
                            onClick = {
                                if (isRegisterMode && fullName.isBlank()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                                if (email.isBlank()) {
                                    errorMessage = "Please enter your email address."
                                    return@Button
                                }
                                if (!isForgotPassword && password.isBlank()) {
                                    errorMessage = "Please enter your password."
                                    return@Button
                                }
                                if (isRegisterMode && password != confirmPassword) {
                                    errorMessage = "Passwords do not match."
                                    return@Button
                                }
                                if (isRegisterMode && password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val fAuth = getFirebaseAuth()
                                        if (isForgotPassword) {
                                            fAuth.sendPasswordResetEmail(email.trim()).await()
                                            successMessage = "Password reset link sent to $email."
                                        } else if (isRegisterMode) {
                                            val result = fAuth.createUserWithEmailAndPassword(email.trim(), password).await()
                                            val user = result.user
                                            val uid = user?.uid
                                            if (uid != null) {
                                                // Update Firebase Auth profile displayName
                                                try {
                                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(fullName.trim())
                                                        .build()
                                                    user.updateProfile(profileUpdates).await()
                                                } catch (eAuthProfile: Exception) {
                                                    Log.e("AuthScreen", "Auth displayName update: ${eAuthProfile.message}")
                                                }

                                                // Save profile into Firestore & Room
                                                val profile = UserProfile(
                                                    userId = uid,
                                                    name = fullName.trim(),
                                                    businessName = businessName.trim().ifBlank { "My Business" },
                                                    mobile = mobile.trim(),
                                                    isSetupComplete = fullName.isNotBlank()
                                                )
                                                if (repository != null) {
                                                    try {
                                                        repository.saveUserProfile(profile)
                                                    } catch (eProfile: Exception) {
                                                        Log.e("AuthScreen", "Profile save error: ${eProfile.message}")
                                                    }
                                                }
                                            }
                                            onAuthSuccess()
                                        } else {
                                            val result = fAuth.signInWithEmailAndPassword(email.trim(), password).await()
                                            val uid = result.user?.uid
                                            if (uid != null && repository != null) {
                                                try {
                                                    repository.fetchAndCacheUserProfile(uid)
                                                } catch (eSync: Exception) {
                                                    Log.e("AuthScreen", "Profile sync on sign in: ${eSync.message}")
                                                }
                                            }
                                            onAuthSuccess()
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        val msg = e.localizedMessage ?: e.message ?: ""
                                        Log.e("AuthScreen", "Auth error: $msg", e)
                                        errorMessage = when {
                                            msg.contains("already in use", ignoreCase = true) || msg.contains("EMAIL_EXISTS", ignoreCase = true) ->
                                                "This email is already registered. Please sign in or reset password."
                                            msg.contains("badly formatted", ignoreCase = true) || msg.contains("invalid-email", ignoreCase = true) ->
                                                "Please enter a valid email address."
                                            msg.contains("password", ignoreCase = true) && (msg.contains("weak", ignoreCase = true) || msg.contains("characters", ignoreCase = true)) ->
                                                "Password must be at least 6 characters."
                                            msg.contains("credential", ignoreCase = true) ||
                                            msg.contains("invalid-credential", ignoreCase = true) ||
                                            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                                            msg.contains("wrong-password", ignoreCase = true) ||
                                            msg.contains("user-not-found", ignoreCase = true) ||
                                            msg.contains("no user record", ignoreCase = true) ||
                                            msg.contains("USER_NOT_FOUND", ignoreCase = true) ->
                                                "Incorrect email or password."
                                            msg.contains("network", ignoreCase = true) ->
                                                "Network error. Please check your internet connection."
                                            else -> "Authentication failed: $msg"
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = when {
                                        isForgotPassword -> "Send Reset Link"
                                        isRegisterMode -> "Create Account"
                                        else -> "Sign In"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isForgotPassword) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    isForgotPassword = false
                                    errorMessage = null
                                    successMessage = null
                                }
                            ) {
                                Text("Back to Login", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
