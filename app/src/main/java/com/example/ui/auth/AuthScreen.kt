package com.example.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val auth = remember {
        try {
            Firebase.auth
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title / Branding
            Text(
                text = "🚜 Time Bill",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Professional Time Tracking & Billing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            isForgotPassword -> "Reset Password"
                            isRegisterMode -> "Create Account"
                            else -> "Welcome Back"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (successMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = successMessage!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business / Service Name") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isForgotPassword) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            if (auth == null) {
                                errorMessage = "Authentication service is currently unavailable."
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
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    if (isForgotPassword) {
                                        auth.sendPasswordResetEmail(email.trim()).await()
                                        successMessage = "Password reset link sent to $email."
                                        isForgotPassword = false
                                    } else if (isRegisterMode) {
                                        if (password != confirmPassword) {
                                            errorMessage = "Passwords do not match."
                                            isLoading = false
                                            return@launch
                                        }
                                        if (password.length < 6) {
                                            errorMessage = "Password must be at least 6 characters."
                                            isLoading = false
                                            return@launch
                                        }
                                        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                                        val uid = result.user?.uid
                                        if (uid != null && repository != null) {
                                            val profile = UserProfile(
                                                userId = uid,
                                                name = fullName.trim(),
                                                businessName = businessName.trim(),
                                                mobile = mobile.trim(),
                                                isSetupComplete = fullName.isNotBlank()
                                            )
                                            repository.saveUserProfile(profile)
                                        }
                                        onAuthSuccess()
                                    } else {
                                        auth.signInWithEmailAndPassword(email.trim(), password).await()
                                        onAuthSuccess()
                                    }
                                } catch (e: Exception) {
                                    val msg = e.localizedMessage ?: ""
                                    errorMessage = when {
                                        msg.contains("already in use", ignoreCase = true) -> "This email is already registered. Please login."
                                        msg.contains("no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) -> "No account found with this email. Please register."
                                        msg.contains("password", ignoreCase = true) && (msg.contains("weak", ignoreCase = true) || msg.contains("characters", ignoreCase = true)) -> "Password must be at least 6 characters."
                                        msg.contains("credential", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true) || msg.contains("password is invalid", ignoreCase = true) -> "Invalid email or password."
                                        else -> msg.ifBlank { "Authentication failed. Please check details." }
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
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    isForgotPassword -> "Send Reset Link"
                                    isRegisterMode -> "Create Account"
                                    else -> "Login"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            isForgotPassword = !isForgotPassword
                            errorMessage = null
                            successMessage = null
                        }) {
                            Text(if (isForgotPassword) "Back to Login" else "Forgot Password?")
                        }

                        TextButton(onClick = {
                            isRegisterMode = !isRegisterMode
                            isForgotPassword = false
                            errorMessage = null
                            successMessage = null
                        }) {
                            Text(if (isRegisterMode) "Existing User? Login" else "Create Account")
                        }
                    }
                }
            }
        }
    }
}
