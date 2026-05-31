package com.activitypoints.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.activitypoints.models.Student
import com.activitypoints.ui.components.InitialsAvatar
import com.activitypoints.ui.components.PhotoViewerDialog
import com.activitypoints.ui.components.ShimmerBox
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState   by profileViewModel.uiState.collectAsState()
    val student   = (authState as? AuthState.Student)?.profile
    val context   = LocalContext.current
    val snackbar  = remember { SnackbarHostState() }

    var showPhotoViewer by remember { mutableStateOf(false) }
    if (showPhotoViewer && student?.photoUrl != null) {
        PhotoViewerDialog(photoUrl = student.photoUrl, onDismiss = { showPhotoViewer = false })
    }

    LaunchedEffect(uiState.error) { uiState.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(uiState.updatedStudent) {
        uiState.updatedStudent?.let {
            authViewModel.refreshStudentProfile(it)
            snackbar.showSnackbar("Photo updated!")
            profileViewModel.clearResult()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { profileViewModel.uploadStudentPhoto(it, context) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Hero banner (mirrors RN ProfileScreen hero) ────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF2D52B0)),
                        )
                    ),
            ) {
                // Back button
                IconButton(
                    onClick  = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, start = 4.dp),
                ) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White)
                }

                // Page title
                Text(
                    text      = "My Profile",
                    color     = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize  = 18.sp,
                    modifier  = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp),
                )

                // Avatar — centered, sits half-inside the banner
                Box(
                    modifier         = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Photo or initials
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable(enabled = student?.photoUrl != null) { showPhotoViewer = true },
                    ) {
                        if (student?.photoUrl != null) {
                            AsyncImage(
                                model              = student.photoUrl,
                                contentDescription = "Profile photo",
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop,
                            )
                        } else {
                            InitialsAvatar(name = student?.name ?: "?", size = 100)
                        }
                    }

                    // Camera badge
                    SmallFloatingActionButton(
                        onClick        = { photoLauncher.launch("image/*") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier       = Modifier
                            .size(30.dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        if (uiState.isUploadingPhoto) {
                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.CameraAlt, "Change photo", Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                }
            }

            // Space so content starts below the avatar overlap
            Spacer(Modifier.height(60.dp))

            // Name + register number
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text       = student?.name ?: "",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = student?.registerNumber ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                student?.branchName?.let {
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (student?.isLateralEntry == true) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text       = "Lateral Entry",
                            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Info card ──────────────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Student Information",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider()
                    ProfileRow(Icons.Outlined.Email,             "Email",        student?.email)
                    ProfileRow(Icons.Outlined.Apartment,         "Branch",       student?.branchName)
                    ProfileRow(Icons.Outlined.CalendarViewMonth, "Batch",        student?.batchName)
                    ProfileRow(Icons.Outlined.School,            "Semester",     student?.semester)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tutor card ─────────────────────────────────────────────────────
            TutorInfoCard(profileViewModel)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TutorInfoCard(profileViewModel: ProfileViewModel) {
    val uiState by profileViewModel.uiState.collectAsState()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "My Tutor",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()

            if (uiState.isTutorLoading) {
                ShimmerBox(Modifier.fillMaxWidth().height(56.dp))
            } else if (uiState.tutor == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.PersonOff, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No tutor assigned", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val t = uiState.tutor!!
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (t.photoUrl != null) {
                        AsyncImage(
                            model              = t.photoUrl,
                            contentDescription = null,
                            modifier           = Modifier.size(44.dp).clip(CircleShape),
                            contentScale       = ContentScale.Crop,
                        )
                    } else {
                        InitialsAvatar(name = t.name, size = 44)
                    }
                    Column {
                        Text(t.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(t.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}