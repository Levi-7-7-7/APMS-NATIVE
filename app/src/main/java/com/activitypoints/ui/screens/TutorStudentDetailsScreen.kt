package com.activitypoints.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.activitypoints.models.Certificate
import com.activitypoints.ui.components.*
import com.activitypoints.utils.CalcPoints
import com.activitypoints.viewmodel.TutorViewModel

private val CERT_FILTERS = listOf("all", "approved", "pending", "rejected")

@Composable
fun TutorStudentDetailsScreen(
    studentId: String,
    navController: NavController,
    tutorViewModel: TutorViewModel = hiltViewModel(),
) {
    val uiState by tutorViewModel.detailState.collectAsState()

    LaunchedEffect(studentId) {
        tutorViewModel.loadStudentDetail(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title         = { Text(uiState.student?.name ?: "Student Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val student = uiState.student
        val certs   = tutorViewModel.filteredDetailCerts()
        val approved = uiState.certificates.filter { it.status.lowercase() == "approved" }
        val totalPts = CalcPoints.calcCappedPoints(approved, isLateralEntry = student?.isLateralEntry ?: false)
        val threshold = CalcPoints.passThreshold(student?.isLateralEntry ?: false)

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile card
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = student?.name ?: "?", size = 52)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(student?.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(student?.registerNumber ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                student?.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        PointsProgressBar(current = totalPts, required = threshold, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = if (totalPts >= threshold) "✅ Requirement Met" else "⚠️ Below requirement",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (totalPts >= threshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CERT_FILTERS) { filter ->
                        FilterChip(
                            selected = uiState.activeFilter == filter,
                            onClick  = { tutorViewModel.setDetailFilter(filter) },
                            label    = { Text(filter.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }

            // Certificates
            if (certs.isEmpty()) {
                item { EmptyState(Icons.Outlined.WorkspacePremium, "No certificates", "No ${uiState.activeFilter} certificates.", Modifier.fillMaxWidth()) }
            } else {
                items(certs, key = { it.id }) { cert ->
                    DetailCertCard(cert)
                }
            }
        }
    }
}

@Composable
private fun DetailCertCard(cert: Certificate) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cert.eventName ?: cert.subcategory ?: "Certificate", fontWeight = FontWeight.SemiBold)
                cert.category?.name?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val pts = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints
                if (pts != null) Text("$pts pts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                cert.rejectionReason?.let {
                    Text("Reason: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            StatusBadge(cert.status)
        }
    }
}
