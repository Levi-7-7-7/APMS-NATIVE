package com.activitypoints.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.activitypoints.data.api.NetworkResult
import com.activitypoints.data.api.safeApiCall
import com.activitypoints.data.local.TokenStore
import com.activitypoints.models.Student
import com.activitypoints.ui.components.InitialsAvatar
import com.activitypoints.utils.ImageCompressor
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState  by authViewModel.authState.collectAsState()
    val uiState    by profileViewModel.uiState.collectAsState()
    val student    = (authState as? AuthState.Student)?.profile
    val context    = LocalContext.current
    val snackbar   = remember { SnackbarHostState() }
    val scope      = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.updatedStudent) {
        uiState.updatedStudent?.let {
            authViewModel.refreshStudentProfile(it)
            snackbar.showSnackbar("Profile updated!")
            profileViewModel.clearResult()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileViewModel.uploadStudentPhoto(it, context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Outlined.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Avatar ─────────────────────────────────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                if (student?.photoUrl != null) {
                    AsyncImage(
                        model             = student.photoUrl,
                        contentDescription = "Profile photo",
                        modifier          = Modifier.size(100.dp).clip(CircleShape),
                        contentScale      = ContentScale.Crop,
                    )
                } else {
                    InitialsAvatar(name = student?.name ?: "?", size = 100)
                }
                SmallFloatingActionButton(
                    onClick           = { photoLauncher.launch("image/*") },
                    containerColor    = MaterialTheme.colorScheme.primary,
                    modifier          = Modifier.size(32.dp),
                ) {
                    if (uiState.isUploadingPhoto) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.CameraAlt, "Change photo", Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(student?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(student?.registerNumber ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            student?.department?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))

            // ── Info cards ─────────────────────────────────────────────────────
            ProfileInfoCard(student)

            Spacer(Modifier.height(16.dp))

            // ── Edit phone ─────────────────────────────────────────────────────
            var phone by remember(student?.phone) { mutableStateOf(student?.phone ?: "") }
            OutlinedTextField(
                value         = phone,
                onValueChange = { phone = it },
                label         = { Text("Phone Number") },
                leadingIcon   = { Icon(Icons.Outlined.Phone, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = { profileViewModel.updateStudentPhone(phone) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled  = !uiState.isUpdating,
            ) {
                if (uiState.isUpdating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(student: Student?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileRow(Icons.Outlined.Email, "Email", student?.email)
            ProfileRow(Icons.Outlined.Apartment, "Department", student?.department)
            ProfileRow(Icons.Outlined.CalendarViewMonth, "Batch", student?.batch)
            ProfileRow(Icons.Outlined.School, "Semester", student?.semester)
            if (student?.isLateralEntry == true) {
                ProfileRow(Icons.Outlined.TransferWithinAStation, "Lateral Entry", "Yes")
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
