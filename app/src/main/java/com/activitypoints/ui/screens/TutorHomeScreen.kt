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
import com.activitypoints.viewmodel.TutorViewModel

private data class TutorTab(val label: String, val icon: ImageVector)

private val TUTOR_TABS = listOf(
    TutorTab("Students",   Icons.Outlined.Group),
    TutorTab("Upload CSV", Icons.Outlined.UploadFile),
    TutorTab("Pending",    Icons.Outlined.HourglassEmpty),
    TutorTab("Approved",   Icons.Outlined.CheckCircle),
)

@Composable
fun TutorHomeScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    tutorViewModel: TutorViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TUTOR_TABS.forEachIndexed { index, tab ->
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
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> TutorStudentsScreen(
                    tutorViewModel = tutorViewModel,
                    authViewModel  = authViewModel,
                    onStudentClick = { id -> navController.navigate(Routes.studentDetail(id)) },
                    onProfileClick = { navController.navigate(Routes.TUTOR_PROFILE) },
                )
                1 -> TutorUploadCsvScreen(tutorViewModel = tutorViewModel)
                2 -> TutorPendingScreen(tutorViewModel = tutorViewModel)
                3 -> TutorApprovedScreen(tutorViewModel = tutorViewModel)
            }
        }
    }
}
