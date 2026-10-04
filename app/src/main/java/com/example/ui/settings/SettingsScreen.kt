package com.example.ui.settings

import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.TimeBillRepository
import com.example.ui.navigation.Screen
import com.example.ui.util.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUserId: String,
    repository: TimeBillRepository,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()
    val userProfileState = repository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val userProfile = userProfileState.value

    var showLogoutDialog by remember { mutableStateOf(false) }
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
                repository.fetchAndCacheUserProfile(currentUserId)
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
                    repository.saveUserProfile(updatedProfile)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Account & Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card with rounded ripple clip
            val profileCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(profileCardShape),
                shape = profileCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                editName = userProfile?.name?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName ?: ""
                                editBusinessName = userProfile?.businessName ?: ""
                                editMobile = userProfile?.mobile ?: ""
                                editAddress = userProfile?.address ?: ""
                                editDefaultService = userProfile?.defaultService ?: "Tractor Ploughing"
                                editDefaultRate = userProfile?.defaultRate?.toInt()?.toString() ?: "500"
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
                            .size(105.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
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

                        // Bottom-Right Camera Icon Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    val displayName = userProfile?.name?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Owner"
                    val displayBusiness = userProfile?.businessName?.takeIf { it.isNotBlank() } ?: "My Business"
                    val displayEmail = auth.currentUser?.email ?: "Not set"
                    val displayMobile = userProfile?.mobile?.takeIf { it.isNotBlank() } ?: auth.currentUser?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set"
                    val displayAddress = userProfile?.address?.takeIf { it.isNotBlank() } ?: "Not set"
                    val displayService = userProfile?.defaultService?.takeIf { it.isNotBlank() } ?: "Tractor Ploughing"
                    val displayRate = "₹${userProfile?.defaultRate?.toInt() ?: 500}/hr"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileInfoRow(label = "Owner Name", value = displayName)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Business Name", value = displayBusiness)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Email Address", value = displayEmail)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Mobile Number", value = displayMobile)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Village / Location", value = displayAddress)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Default Service", value = displayService)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileInfoRow(label = "Default Hourly Rate", value = displayRate)
                    }
                }
            }

            // Quick App Management Cards with Rounded Ripple shapes
            val manageCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(manageCardShape),
                shape = manageCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Business Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
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
                }
            }

            // Account Actions / Logout Section
            val logoutCardShape = RoundedCornerShape(18.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(logoutCardShape),
                shape = logoutCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Account Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
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
                    OutlinedTextField(
                        value = editMobile,
                        onValueChange = { editMobile = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Village / Location / Address") },
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
                            val current = userProfile ?: UserProfile(userId = currentUserId)
                            val updated = current.copy(
                                userId = currentUserId,
                                name = editName.trim().ifBlank { "Owner" },
                                businessName = editBusinessName.trim().ifBlank { "My Business" },
                                mobile = editMobile.trim(),
                                address = editAddress.trim(),
                                defaultService = editDefaultService.trim().ifBlank { "Tractor Ploughing" },
                                defaultRate = editDefaultRate.toDoubleOrNull() ?: 500.0,
                                isSetupComplete = true
                            )
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
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
