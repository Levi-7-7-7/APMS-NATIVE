package com.activitypoints.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.activitypoints.navigation.Routes
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.StudentViewModel

// Tab definition
private data class StudentTab(val label: String, val icon: ImageVector)

private val STUDENT_TABS = listOf(
    StudentTab("Dashboard",    Icons.Outlined.Dashboard),
    StudentTab("Certificates", Icons.Outlined.WorkspacePremium),
    StudentTab("Upload",       Icons.Outlined.UploadFile),
)

@Composable
fun StudentHomeScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    studentViewModel: StudentViewModel = hiltViewModel(),
) {
    // Wire the AuthViewModel so that loadAll() can push profile updates into authState.
    LaunchedEffect(Unit) {
        studentViewModel.attachAuthViewModel(authViewModel)
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                STUDENT_TABS.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        icon     = { Icon(tab.icon, contentDescription = tab.label) },
                        label    = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    authViewModel    = authViewModel,
                    studentViewModel = studentViewModel,
                    onNavigateToProfile = { navController.navigate(Routes.STUDENT_PROFILE) },
                    onNavigateToCerts   = { selectedTab = 1 },
                )
                1 -> CertificatesScreen(
                    studentViewModel = studentViewModel,
                )
                2 -> UploadCertificateScreen(
                    studentViewModel = studentViewModel,
                    onUploadSuccess  = { selectedTab = 1 },
                )
            }
        }
    }
}
