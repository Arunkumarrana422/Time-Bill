package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.util.clearFocusOnTap
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

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

    var isEmailAuthLoading by remember { mutableStateOf(false) }
    var isGoogleAuthLoading by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var authBannerError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun clearAllErrors() {
        emailError = null
        passwordError = null
        fullNameError = null
        confirmPasswordError = null
        authBannerError = null
        successMessage = null
    }

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

    fun handleFirebaseSignInWithIdToken(idToken: String, accountName: String?, photoUrl: String?) {
        isGoogleAuthLoading = true
        authBannerError = null
        successMessage = null
        scope.launch {
            try {
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val fAuth = try { Firebase.auth } catch (e: Exception) { getFirebaseAuth() }
                val authResult = fAuth.signInWithCredential(authCredential).await()
                val user = authResult.user
                val uid = user?.uid

                if (uid != null) {
                    val displayName = user.displayName ?: accountName ?: ""
                    val photo = user.photoUrl?.toString() ?: photoUrl ?: ""

                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val existing = repository?.getUser(uid)
                            val profile = existing ?: UserProfile(
                                userId = uid,
                                name = displayName.ifBlank { "Google User" },
                                businessName = "My Business",
                                mobile = "",
                                isSetupComplete = displayName.isNotBlank(),
                                profilePhotoUri = photo
                            )
                            repository?.saveUserProfile(profile)
                            repository?.fetchAndCacheUserProfile(uid)
                        } catch (eProfile: Exception) {
                            Log.e("AuthScreen", "Profile save on Google sign in: ${eProfile.message}")
                        }
                    }
                }
                onAuthSuccess()
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: e.toString()
                Log.e("AuthScreen", "Firebase credential sign-in error: $msg", e)
                authBannerError = "Firebase Login Error: $msg"
            } finally {
                isGoogleAuthLoading = false
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                handleFirebaseSignInWithIdToken(idToken, account.displayName, account.photoUrl?.toString())
            } else {
                authBannerError = "Failed to retrieve Google token. Please try again."
                isGoogleAuthLoading = false
            }
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            Log.e("AuthScreen", "Google Sign In ApiException status: $statusCode", e)
            authBannerError = when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> null
                GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                    "Google Play Services (10): Google is propagating your SHA-1 key on cloud servers. Please wait 1-2 minutes and tap again."
                GoogleSignInStatusCodes.NETWORK_ERROR ->
                    "Network error. Please check your internet connection."
                else -> "Google Sign-In failed (Code $statusCode): ${e.localizedMessage}"
            }
            isGoogleAuthLoading = false
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: e.toString()
            Log.e("AuthScreen", "Google Sign-In general error: $msg", e)
            authBannerError = "Sign-in error: $msg"
            isGoogleAuthLoading = false
        }
    }

    fun signInWithGoogle() {
        isGoogleAuthLoading = true
        authBannerError = null
        successMessage = null
        try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .requestEmail()
                .requestProfile()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            // Sign out first to ensure user can pick any account cleanly every time
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("AuthScreen", "Error launching Google Sign In: ${e.message}", e)
            authBannerError = "Could not start Google Sign In: ${e.message}"
            isGoogleAuthLoading = false
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
                                        clearAllErrors()
                                    },
                                    text = { Text("Login", fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal) }
                                )
                                Tab(
                                    selected = isRegisterMode,
                                    onClick = {
                                        isRegisterMode = true
                                        clearAllErrors()
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

                        if (authBannerError != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = authBannerError!!,
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
                            val isFullNameError = fullNameError != null
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { 
                                    fullName = it
                                    if (fullNameError != null) fullNameError = null
                                },
                                label = { Text("Full Name *") },
                                isError = isFullNameError,
                                supportingText = {
                                    if (isFullNameError) {
                                        Text(fullNameError ?: "Full name is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business / Service Name (Optional)") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val isMobileError = mobile.isNotEmpty() && mobile.length < 10
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(10)
                                    mobile = digits
                                },
                                label = { Text("Mobile Number (Optional)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = isMobileError,
                                supportingText = {
                                    if (isMobileError) {
                                        Text("Mobile number must be 10 digits (${mobile.length}/10)", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val isEmailError = emailError != null
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                if (emailError != null) emailError = null
                            },
                            label = { Text("Email Address *") },
                            isError = isEmailError,
                            supportingText = {
                                if (isEmailError) {
                                    Text(emailError ?: "Email address is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isForgotPassword) {
                            val isPasswordError = passwordError != null
                            OutlinedTextField(
                                value = password,
                                onValueChange = { 
                                    password = it
                                    if (passwordError != null) passwordError = null
                                },
                                label = { Text("Password *") },
                                isError = isPasswordError,
                                supportingText = {
                                    if (isPasswordError) {
                                        Text(passwordError ?: "Password is required *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                },
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
                                            clearAllErrors()
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
                            val isConfirmPasswordError = confirmPasswordError != null
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    if (confirmPasswordError != null) confirmPasswordError = null
                                },
                                label = { Text("Confirm Password *") },
                                isError = isConfirmPasswordError,
                                supportingText = {
                                    if (isConfirmPasswordError) {
                                        Text(confirmPasswordError ?: "Passwords do not match *", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                },
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
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Main Action Button
                        Button(
                            onClick = {
                                var hasFormError = false
                                if (isRegisterMode && fullName.isBlank()) {
                                    fullNameError = "Please enter your full name *"
                                    hasFormError = true
                                }
                                if (email.isBlank()) {
                                    emailError = "Please enter your email address *"
                                    hasFormError = true
                                } else if (!email.contains("@") || !email.contains(".")) {
                                    emailError = "Please enter a valid email address *"
                                    hasFormError = true
                                }
                                if (!isForgotPassword && password.isBlank()) {
                                    passwordError = "Please enter your password *"
                                    hasFormError = true
                                } else if (isRegisterMode && password.length < 6) {
                                    passwordError = "Password must be at least 6 characters *"
                                    hasFormError = true
                                }
                                if (isRegisterMode && confirmPassword.isBlank()) {
                                    confirmPasswordError = "Please confirm your password *"
                                    hasFormError = true
                                } else if (isRegisterMode && password != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match *"
                                    hasFormError = true
                                }

                                if (hasFormError) {
                                    return@Button
                                }

                                isEmailAuthLoading = true
                                authBannerError = null
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
                                        authBannerError = when {
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
                                        isEmailAuthLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isEmailAuthLoading && !isGoogleAuthLoading
                        ) {
                            if (isEmailAuthLoading) {
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

                        if (!isForgotPassword) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Or divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "  OR  ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Google Sign In Button
                            OutlinedButton(
                                onClick = { signInWithGoogle() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isEmailAuthLoading && !isGoogleAuthLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                if (isGoogleAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google_logo),
                                            contentDescription = "Google Logo",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Sign in with Google",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (isForgotPassword) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    isForgotPassword = false
                                    clearAllErrors()
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
