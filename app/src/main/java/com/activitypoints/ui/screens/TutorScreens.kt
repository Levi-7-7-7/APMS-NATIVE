package com.activitypoints.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.activitypoints.models.TutorApprovedCert
import com.activitypoints.models.TutorPendingCert
import com.activitypoints.models.TutorStudent
import com.activitypoints.ui.components.*
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.TutorViewModel
import java.io.File
import java.io.FileOutputStream

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR STUDENTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorStudentsScreen(
    tutorViewModel: TutorViewModel,
    authViewModel: AuthViewModel,
    onStudentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
) {
    val uiState  by tutorViewModel.studentsState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val tutorName = (authState as? AuthState.Tutor)?.profile?.name ?: "Tutor"

    Column(Modifier.fillMaxSize()) {
        // Header row
        Row(
            modifier             = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Column {
                Text("Hello, $tutorName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("My Students", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = onProfileClick) { Icon(Icons.Outlined.AccountCircle, "Profile") }
                IconButton(onClick = { authViewModel.logout() }) { Icon(Icons.Outlined.Logout, "Logout") }
            }
        }

        // Search
        OutlinedTextField(
            value         = uiState.searchQuery,
            onValueChange = tutorViewModel::setStudentSearch,
            label         = { Text("Search students") },
            leadingIcon   = { Icon(Icons.Outlined.Search, null) },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine    = true,
        )
        Spacer(Modifier.height(8.dp))

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh    = { tutorViewModel.loadStudents(isRefresh = true) },
        ) {
            if (uiState.isLoading) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(72.dp)) }
                }
            } else {
                val filtered = tutorViewModel.filteredStudents()
                if (filtered.isEmpty()) {
                    EmptyState(Icons.Outlined.Group, "No students", "No students match your search.", Modifier.fillMaxSize())
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered, key = { it.id }) { student ->
                            StudentCard(student, onClick = { onStudentClick(student.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCard(student: TutorStudent, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = student.name, size = 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(student.registerNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${student.totalApprovedPoints}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR PENDING
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorPendingScreen(tutorViewModel: TutorViewModel) {
    val uiState by tutorViewModel.pendingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let {
            snackbarHostState.showSnackbar(it)
            tutorViewModel.clearPendingActionResult()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh    = { tutorViewModel.loadPending(isRefresh = true) },
            modifier     = Modifier.padding(padding),
        ) {
            if (uiState.isLoading) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(130.dp)) }
                }
            } else if (uiState.certs.isEmpty()) {
                EmptyState(Icons.Outlined.HourglassEmpty, "No pending certificates", "All caught up!", Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.certs, key = { it.id }) { cert ->
                        PendingCertCard(
                            cert      = cert,
                            onApprove = { pts -> tutorViewModel.approveCertificate(cert.id, pts) },
                            onReject  = { reason -> tutorViewModel.rejectCertificate(cert.id, reason) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingCertCard(
    cert: TutorPendingCert,
    onApprove: (Int) -> Unit,
    onReject: (String) -> Unit,
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog  by remember { mutableStateOf(false) }
    var pointsInput       by remember { mutableStateOf(cert.potentialPoints?.toString() ?: "") }
    var rejectReason      by remember { mutableStateOf("") }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Approve Certificate") },
            text = {
                OutlinedTextField(
                    value         = pointsInput,
                    onValueChange = { pointsInput = it },
                    label         = { Text("Points to award") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onApprove(pointsInput.toIntOrNull() ?: 0)
                    showApproveDialog = false
                }) { Text("Approve") }
            },
            dismissButton = { TextButton(onClick = { showApproveDialog = false }) { Text("Cancel") } },
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Certificate") },
            text = {
                OutlinedTextField(
                    value         = rejectReason,
                    onValueChange = { rejectReason = it },
                    label         = { Text("Rejection reason") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") } },
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(cert.eventName ?: cert.subcategory ?: "Certificate", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            cert.student?.let { s ->
                Text("${s.name} · ${s.registerNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            cert.category?.name?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            Text("Potential: ${cert.potentialPoints ?: 0} pts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showApproveDialog = true }, modifier = Modifier.weight(1f)) { Text("Approve") }
                OutlinedButton(
                    onClick  = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reject") }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR APPROVED
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorApprovedScreen(tutorViewModel: TutorViewModel) {
    val uiState by tutorViewModel.approvedState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh    = { tutorViewModel.loadApproved(isRefresh = true) },
    ) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = tutorViewModel::setApprovedSearch,
                label         = { Text("Search") },
                leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                modifier      = Modifier.fillMaxWidth().padding(16.dp),
                singleLine    = true,
            )
            val filtered = uiState.certs.filter {
                val q = uiState.searchQuery.trim().lowercase()
                q.isEmpty() || (it.student?.name?.lowercase()?.contains(q) == true) ||
                (it.student?.registerNumber?.lowercase()?.contains(q) == true) ||
                (it.eventName?.lowercase()?.contains(q) == true)
            }
            if (filtered.isEmpty()) {
                EmptyState(Icons.Outlined.CheckCircle, "No approved certificates", "", Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { cert ->
                        ApprovedCertCard(cert)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovedCertCard(cert: TutorApprovedCert) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cert.eventName ?: cert.subcategory ?: "Certificate", fontWeight = FontWeight.SemiBold)
                cert.student?.let { Text("${it.name} · ${it.registerNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("${cert.pointsAwarded ?: 0} pts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR UPLOAD CSV
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorUploadCsvScreen(tutorViewModel: TutorViewModel) {
    val context  = LocalContext.current
    val uiState  by tutorViewModel.csvState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.result, uiState.error) {
        (uiState.result ?: uiState.error)?.let {
            snackbar.showSnackbar(it)
            tutorViewModel.clearCsvResult()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = try {
                val name = "upload_${System.currentTimeMillis()}.csv"
                val out  = File(context.cacheDir, name)
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    FileOutputStream(out).use { fos -> ins.copyTo(fos) }
                }
                out
            } catch (e: Exception) { null }
            file?.let { f -> tutorViewModel.uploadCsv(f) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.TableChart, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Upload Student CSV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Upload a CSV file to bulk-import students.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick  = { launcher.launch("text/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isUploading,
            ) {
                if (uiState.isUploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Outlined.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Pick CSV & Upload", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
