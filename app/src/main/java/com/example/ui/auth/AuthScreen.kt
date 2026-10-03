package com.example.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

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

    val auth = Firebase.auth

    fun handleGoogleSignIn() {
        val clientId = context.getString(com.example.R.string.default_web_client_id)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    isLoading = false
                    onAuthSuccess()
                } else {
                    isLoading = false
                    errorMessage = "Unexpected credential type."
                }
            } catch (e: GetCredentialCancellationException) {
                isLoading = false
                Log.w("Auth", "Google sign-in cancelled")
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Google Sign-In failed"
            }
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Professional Time Tracking & Billing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        fontWeight = FontWeight.SemiBold
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
                            if (email.isBlank()) {
                                errorMessage = "Please enter email"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    if (isForgotPassword) {
                                        auth.sendPasswordResetEmail(email).await()
                                        successMessage = "Password reset email sent."
                                        isForgotPassword = false
                                    } else if (isRegisterMode) {
                                        if (password != confirmPassword) {
                                            errorMessage = "Passwords do not match."
                                            isLoading = false
                                            return@launch
                                        }
                                        auth.createUserWithEmailAndPassword(email, password).await()
                                        onAuthSuccess()
                                    } else {
                                        auth.signInWithEmailAndPassword(email, password).await()
                                        onAuthSuccess()
                                    }
                                } catch (e: Exception) {
                                    val msg = e.localizedMessage ?: ""
                                    errorMessage = when {
                                        msg.contains("already in use", ignoreCase = true) -> "This email is already registered. Please sign in instead."
                                        msg.contains("no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) -> "No account found with this email. Please register."
                                        msg.contains("password", ignoreCase = true) && (msg.contains("weak", ignoreCase = true) || msg.contains("characters", ignoreCase = true)) -> "Password should be at least 6 characters."
                                        msg.contains("credential", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true) || msg.contains("password is invalid", ignoreCase = true) -> "Invalid email or password. Please check your credentials."
                                        else -> msg.ifBlank { "Authentication error occurred. Please try again." }
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                    isForgotPassword -> "Send Reset Instructions"
                                    isRegisterMode -> "Create Account"
                                    else -> "Login"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isForgotPassword) {
                        OutlinedButton(
                            onClick = { handleGoogleSignIn() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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
