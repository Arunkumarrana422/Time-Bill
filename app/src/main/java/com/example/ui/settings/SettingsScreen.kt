package com.example.ui.settings

import android.content.Context
import com.example.ui.util.formatIndianCurrency
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import com.example.ui.util.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun deriveNameFromEmail(email: String?): String {
    if (email.isNullOrBlank()) return "Owner"
    val prefix = email.substringBefore("@")
    val cleaned = prefix.filter { it.isLetter() || it == '.' || it == '_' }.replace(".", " ").replace("_", " ").trim()
    return if (cleaned.isNotBlank()) {
        cleaned.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    } else {
        "Owner"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    showBottomBar: Boolean = true,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()
    val userProfileState = repository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val observedProfile = userProfileState.value

    var localProfileOverride by remember { mutableStateOf<UserProfile?>(null) }
    val userProfile = localProfileOverride ?: observedProfile

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEraseDataDialog by remember { mutableStateOf(false) }
    var eraseConfirmationText by remember { mutableStateOf("") }
    var isErasingData by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var isPhotoLoading by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editBusinessName by remember { mutableStateOf("") }
    var editMobile by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editDefaultService by remember { mutableStateOf("") }
    var editDefaultRate by remember { mutableStateOf("") }

    // Instant sync on opening Account / Settings to guarantee registration data & photo appear immediately
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty() && currentUserId != "local_offline_user") {
            try {
                val fetched = repository.fetchAndCacheUserProfile(currentUserId)
                if (fetched != null && localProfileOverride == null) {
                    localProfileOverride = fetched
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Convert and save photo as Base64 in Firestore & Room
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isPhotoLoading = true
                val base64 = ImageUtils.uriToBase64(context, it, maxDimension = 320, quality = 80)
                if (base64 != null) {
                    val current = userProfile ?: UserProfile(userId = currentUserId)
                    val updatedProfile = current.copy(
                        userId = currentUserId,
                        profilePhotoUri = base64
                    )
                    localProfileOverride = updatedProfile
                    repository.saveUserProfile(updatedProfile)
                    Toast.makeText(context, "Photo updated successfully!", Toast.LENGTH_SHORT).show()
                }
                isPhotoLoading = false
            }
        }
    }

    // Decode Base64 bitmap instantly if available
    val decodedBitmap by remember(userProfile?.profilePhotoUri) {
        derivedStateOf {
            val photo = userProfile?.profilePhotoUri
            if (!photo.isNullOrEmpty()) {
                ImageUtils.base64ToBitmap(photo)
            } else null
        }
    }

    val inferredName = remember(userProfile, auth.currentUser) {
        userProfile?.name?.takeIf { it.isNotBlank() && it != "Owner" }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: deriveNameFromEmail(auth.currentUser?.email)
    }

    val inferredBusiness = remember(userProfile, inferredName) {
        userProfile?.businessName?.takeIf { it.isNotBlank() && it != "My Business" }
            ?: if (inferredName.isNotBlank() && inferredName != "Owner") "$inferredName Farm & Machinery Services" else "My Business"
    }

    val displayEmail = auth.currentUser?.email ?: "Not set"
    val displayMobile = userProfile?.mobile?.takeIf { it.isNotBlank() } ?: auth.currentUser?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set"
    val displayAddress = userProfile?.address?.takeIf { it.isNotBlank() } ?: "Not set"
    val displayService = userProfile?.defaultService?.takeIf { it.isNotBlank() } ?: "Tractor Ploughing"
    val displayRate = "₹${formatIndianCurrency(userProfile?.defaultRate ?: 1000.0)}/hr"

    val isProfileIncomplete = (userProfile?.mobile.isNullOrBlank() || userProfile?.address.isNullOrBlank() || userProfile?.name.isNullOrBlank() || userProfile?.name == "Owner")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Account & Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = false,
                        onClick = { onNavigate(Screen.Dashboard.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Work, contentDescription = null) },
                        label = { Text("Jobs") },
                        selected = false,
                        onClick = { onNavigate(Screen.Jobs.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Customers") },
                        selected = false,
                        onClick = { onNavigate(Screen.Customers.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Reports") },
                        selected = false,
                        onClick = { onNavigate(Screen.Reports.route) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = true,
                        onClick = {}
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Setup Incomplete Notice Banner if mobile/address/name not set
            if (isProfileIncomplete) {
                val bannerShape = RoundedCornerShape(14.dp)
                Surface(
                    shape = bannerShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚡ Complete Business Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Set your name, mobile number & village location", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Button(
                            onClick = {
                                editName = inferredName
                                editBusinessName = inferredBusiness
                                editMobile = userProfile?.mobile ?: ""
                                editAddress = userProfile?.address ?: ""
                                editDefaultService = userProfile?.defaultService ?: "Tractor Ploughing"
                                editDefaultRate = (userProfile?.defaultRate ?: 1000.0).toInt().toString()
                                showEditProfileDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Setup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // User Profile Card with Material 3 elevated shadow
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val profileCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = profileCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp, pressedElevation = 7.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Business Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton(
                            onClick = {
                                editName = inferredName
                                editBusinessName = inferredBusiness
                                editMobile = userProfile?.mobile ?: ""
                                editAddress = userProfile?.address ?: ""
                                editDefaultService = userProfile?.defaultService ?: "Tractor Ploughing"
                                editDefaultRate = (userProfile?.defaultRate ?: 1000.0).toInt().toString()
                                showEditProfileDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile")
                        }
                    }

                    // Profile Photo Circle with Camera Badge
                    Box(
                        modifier = Modifier
                            .size(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Main Circular Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (decodedBitmap != null) {
                                Image(
                                    bitmap = decodedBitmap!!.asImageBitmap(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!userProfile?.profilePhotoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile?.profilePhotoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onLoading = { isPhotoLoading = true },
                                    onSuccess = { isPhotoLoading = false },
                                    onError = { isPhotoLoading = false }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Icon",
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (isPhotoLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        // Bottom-Right Camera Icon Badge (Fully round, elevated, never cut off)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileInfoRow(label = "Owner Name", value = inferredName)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Business Name", value = inferredBusiness)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Email Address", value = displayEmail)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(
                            label = "Mobile Number",
                            value = displayMobile,
                            valueColor = if (displayMobile == "Not set") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(
                            label = "Village / Location",
                            value = displayAddress,
                            valueColor = if (displayAddress == "Not set") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Default Hourly Rate", value = displayRate)
                    }
                }
            }

            // Quick App Management Cards with Material 3 elevated shadow
            val manageCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = manageCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp, pressedElevation = 7.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Business Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    SettingsActionItem(
                        icon = Icons.Default.Build,
                        title = "Services & Hourly Rates",
                        subtitle = "Configure machinery & billing rates",
                        onClick = { onNavigate(Screen.Services.route) }
                    )
                    SettingsActionItem(
                        icon = Icons.Default.Receipt,
                        title = "Expenses Manager",
                        subtitle = "Track diesel, repair & maintenance",
                        onClick = { onNavigate(Screen.Expenses.route) }
                    )
                    SettingsActionItem(
                        icon = Icons.Default.Assessment,
                        title = "Reports & Profit Analytics",
                        subtitle = "View earnings, received & pending dues",
                        onClick = { onNavigate(Screen.Reports.route) }
                    )
                    SettingsActionItem(
                        icon = Icons.Default.Lock,
                        title = "Update Password",
                        subtitle = "Change your account password securely",
                        onClick = { onNavigate(Screen.UpdatePassword.route) }
                    )
                }
            }



            // Account Actions / Logout & Erase All Data Section
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clean Outlined Logout Button (Border Only)
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }

                // Clean Outlined Erase All Data Button (Border Only, to the right of Logout)
                OutlinedButton(
                    onClick = {
                        eraseConfirmationText = ""
                        showEraseDataDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("erase_all_data_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Erase All Data",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Erase All Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile & Business Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Owner / Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBusinessName,
                        onValueChange = { editBusinessName = it },
                        label = { Text("Business Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val isMobileError = editMobile.isNotEmpty() && editMobile.length < 10
                    OutlinedTextField(
                        value = editMobile,
                        onValueChange = { editMobile = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("Mobile Number *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isMobileError,
                        supportingText = {
                            if (isMobileError) {
                                Text("Mobile number must be 10 digits (${editMobile.length}/10)", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Village / Location / Address *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDefaultService,
                        onValueChange = { editDefaultService = it },
                        label = { Text("Default Service Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDefaultRate,
                        onValueChange = { editDefaultRate = it },
                        label = { Text("Default Hourly Rate (₹)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val finalName = editName.trim().ifBlank { inferredName }
                            val finalBusiness = editBusinessName.trim().ifBlank { inferredBusiness }
                            val finalMobile = editMobile.trim()
                            if (finalMobile.isNotEmpty() && finalMobile.length < 10) {
                                Toast.makeText(context, "Mobile number must be 10 digits", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val finalAddress = editAddress.trim()
                            val finalService = editDefaultService.trim().ifBlank { "Tractor Ploughing" }
                            val finalRate = editDefaultRate.toDoubleOrNull() ?: 1000.0

                            val current = userProfile ?: UserProfile(userId = currentUserId)
                            val updated = current.copy(
                                userId = currentUserId,
                                name = finalName,
                                businessName = finalBusiness,
                                mobile = finalMobile,
                                address = finalAddress,
                                defaultService = finalService,
                                defaultRate = finalRate,
                                isSetupComplete = true
                            )
                            // Update local state immediately
                            localProfileOverride = updated
                            
                            // Update Firebase user profile displayName
                            try {
                                auth.currentUser?.let { user ->
                                    val profileUpdates = userProfileChangeRequest {
                                        displayName = updated.name
                                    }
                                    user.updateProfile(profileUpdates).await()
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                            repository.saveUserProfile(updated)
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            showEditProfileDialog = false
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Confirmation") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        auth.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEraseDataDialog) {
        val isConfirmed = eraseConfirmationText.trim().equals("DELETE", ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                if (!isErasingData) {
                    showEraseDataDialog = false
                    eraseConfirmationText = ""
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Erase All Data?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "This will permanently delete all data",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val items = listOf(
                                "All Customers & Balances",
                                "All Jobs & Work Records",
                                "All Payments & Invoices",
                                "All Diesel & Expense Entries",
                                "Custom Services"
                            )
                            items.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = item,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Your User Profile & Login Account will remain saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "To confirm permanent deletion, type DELETE below:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = eraseConfirmationText,
                        onValueChange = { eraseConfirmationText = it },
                        placeholder = { Text("Type DELETE here") },
                        singleLine = true,
                        enabled = !isErasingData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("erase_confirmation_input")
                    )

                    if (isErasingData) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Erasing data from device and database...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isErasingData = true
                            try {
                                repository.eraseAllUserDataExceptProfile(currentUserId)
                                Toast.makeText(
                                    context,
                                    "All data has been erased successfully! Profile kept safe.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showEraseDataDialog = false
                                eraseConfirmationText = ""
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error erasing data: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isErasingData = false
                            }
                        }
                    },
                    enabled = isConfirmed && !isErasingData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_erase_button")
                ) {
                    if (isErasingData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Erase All Data")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEraseDataDialog = false
                        eraseConfirmationText = ""
                    },
                    enabled = !isErasingData
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = itemShape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
