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
import com.activitypoints.ui.components.InitialsAvatar
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.TutorProfileViewModel

@Composable
fun TutorProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    tutorProfileViewModel: TutorProfileViewModel = hiltViewModel(),
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState   by tutorProfileViewModel.uiState.collectAsState()
    val tutor     = (authState as? AuthState.Tutor)?.profile
    val snackbar  = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(uiState.updatedTutor) {
        uiState.updatedTutor?.let {
            authViewModel.refreshTutorProfile(it)
            snackbar.showSnackbar("Photo updated!")
            tutorProfileViewModel.clearResult()
        }
    }

    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tutorProfileViewModel.uploadPhoto(it, context) }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                if (tutor?.photoUrl != null) {
                    AsyncImage(
                        model             = tutor.photoUrl,
                        contentDescription = "Tutor photo",
                        modifier          = Modifier.size(100.dp).clip(CircleShape),
                        contentScale      = ContentScale.Crop,
                    )
                } else {
                    InitialsAvatar(name = tutor?.name ?: "T", size = 100)
                }
                SmallFloatingActionButton(
                    onClick        = { photoLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier       = Modifier.size(32.dp),
                ) {
                    if (uiState.isUploading) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.CameraAlt, "Change photo", Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(tutor?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            tutor?.email?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            tutor?.department?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick  = { authViewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
