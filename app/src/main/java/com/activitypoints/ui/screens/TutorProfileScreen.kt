package com.activitypoints.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.activitypoints.ui.components.InitialsAvatar
import com.activitypoints.ui.components.PhotoViewerDialog
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.TutorProfileViewModel
import com.activitypoints.viewmodel.TutorViewModel

@Composable
fun TutorProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    tutorViewModel: TutorViewModel,
    tutorProfileViewModel: TutorProfileViewModel = hiltViewModel(),
) {
    val authState      by authViewModel.authState.collectAsState()
    val uiState        by tutorProfileViewModel.uiState.collectAsState()
    val studentsState  by tutorViewModel.studentsState.collectAsState()
    val tutor          = (authState as? AuthState.Tutor)?.profile
    val snackbar       = remember { SnackbarHostState() }
    val context        = LocalContext.current
    val studentCount   = studentsState.students.size

    var showPhotoViewer by remember { mutableStateOf(false) }
    if (showPhotoViewer && tutor?.photoUrl != null) {
        PhotoViewerDialog(photoUrl = tutor.photoUrl, onDismiss = { showPhotoViewer = false })
    }

    LaunchedEffect(uiState.error) { uiState.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(uiState.updatedTutor) {
        uiState.updatedTutor?.let {
            authViewModel.refreshTutorProfile(it)
            snackbar.showSnackbar("Photo updated!")
            tutorProfileViewModel.clearResult()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { tutorProfileViewModel.uploadPhoto(it, context) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Hero banner ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F2041), Color(0xFF1E3A8A)),
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

                Text(
                    text       = "My Profile",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    modifier   = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp),
                )

                // Avatar
                Box(
                    modifier         = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (tutor?.photoUrl != null) {
                        AsyncImage(
                            model              = tutor.photoUrl,
                            contentDescription = "Tutor photo",
                            modifier           = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale       = ContentScale.Crop,
                        )
                    } else {
                        InitialsAvatar(name = tutor?.name ?: "T", size = 100)
                    }

                    SmallFloatingActionButton(
                        onClick        = { photoLauncher.launch("image/*") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier       = Modifier
                            .size(30.dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        if (uiState.isUploading) {
                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.CameraAlt, "Change photo", Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                }
            }

            // Space for avatar overlap
            Spacer(Modifier.height(60.dp))

            // Name + email
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text       = tutor?.name ?: "",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                tutor?.email?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tutor?.department?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats row: student count + batch + branch ──────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TutorStatChip(
                    icon  = Icons.Outlined.People,
                    value = "$studentCount",
                    label = "Students",
                    modifier = Modifier.weight(1f),
                )
                tutor?.batch?.name?.let { batch ->
                    TutorStatChip(
                        icon  = Icons.Outlined.CalendarViewMonth,
                        value = batch,
                        label = "Batch",
                        modifier = Modifier.weight(1f),
                    )
                }
                tutor?.branch?.name?.let { branch ->
                    TutorStatChip(
                        icon  = Icons.Outlined.School,
                        value = branch,
                        label = "Branch",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

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
                        "Tutor Information",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider()
                    TutorProfileRow(Icons.Outlined.Email,             "Email",   tutor?.email)
                    TutorProfileRow(Icons.Outlined.Apartment,         "Branch",  tutor?.branch?.name)
                    TutorProfileRow(Icons.Outlined.CalendarViewMonth, "Batch",   tutor?.batch?.name)
                    TutorProfileRow(Icons.Outlined.Category,          "Department", tutor?.department)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TutorStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text       = value,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun TutorProfileRow(
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