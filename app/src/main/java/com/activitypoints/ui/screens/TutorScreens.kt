package com.activitypoints.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.activitypoints.models.Certificate
import com.activitypoints.models.TutorPendingCert
import com.activitypoints.models.TutorStudent
import com.activitypoints.ui.components.*
import com.activitypoints.viewmodel.*
import java.io.File
import java.io.FileOutputStream

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR STUDENTS LIST
// ══════════════════════════════════════════════════════════════════════════════

private val SORT_OPTIONS = listOf(
    StudentSortKey.REGISTER_NUMBER to "Reg. Number",
    StudentSortKey.NAME            to "Name",
    StudentSortKey.TOTAL_POINTS    to "Points",
    StudentSortKey.BATCH           to "Batch",
    StudentSortKey.BRANCH          to "Branch",
    StudentSortKey.RECENTLY_ADDED  to "Recently Added",
)

@Composable
fun TutorStudentsScreen(
    tutorViewModel: TutorViewModel,
    authViewModel: com.activitypoints.viewmodel.AuthViewModel,
    onStudentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
) {
    val uiState by tutorViewModel.studentsState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showSortSheet   by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val displayed = tutorViewModel.filteredSortedStudents()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Students") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Outlined.AccountCircle, "Profile")
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Outlined.Logout, "Logout")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = tutorViewModel::setStudentSearch,
                placeholder   = { Text("Search by name or reg. number…") },
                leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                trailingIcon  = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { tutorViewModel.setStudentSearch("") }) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                },
                singleLine  = true,
                modifier    = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape       = RoundedCornerShape(12.dp),
            )

            // Sort / filter toolbar
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = true,
                    onClick  = { showSortSheet = true },
                    label    = { Text(SORT_OPTIONS.find { it.first == uiState.sortKey }?.second ?: "Sort") },
                    leadingIcon = {
                        Icon(
                            if (uiState.sortDir == SortDir.ASC) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                            null,
                            Modifier.size(16.dp),
                        )
                    },
                )

                val activeFilters = listOfNotNull(
                    uiState.filterBatch.takeIf { it.isNotBlank() },
                    uiState.filterBranch.takeIf { it.isNotBlank() },
                ).size

                FilterChip(
                    selected    = activeFilters > 0,
                    onClick     = { showFilterSheet = true },
                    label       = { Text(if (activeFilters > 0) "Filter ($activeFilters)" else "Filter") },
                    leadingIcon = { Icon(Icons.Outlined.FilterList, null, Modifier.size(16.dp)) },
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${displayed.size}/${uiState.students.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh    = { tutorViewModel.loadStudents(isRefresh = true) },
                modifier     = Modifier.weight(1f),
            ) {
                if (uiState.isLoading) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(80.dp)) }
                    }
                } else if (displayed.isEmpty()) {
                    EmptyState(Icons.Outlined.PersonSearch, "No students found", "", Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(displayed, key = { it.id }) { student ->
                            StudentCard(
                                student   = student,
                                onClick   = { onStudentClick(student.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Sort bottom sheet
    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Sort Students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SORT_OPTIONS.forEach { (key, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            if (uiState.sortKey == key) {
                                Icon(
                                    if (uiState.sortDir == SortDir.ASC) Icons.Outlined.ArrowUpward
                                    else Icons.Outlined.ArrowDownward,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            tutorViewModel.setStudentSort(key)
                            showSortSheet = false
                        },
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Filter Students", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { tutorViewModel.clearStudentFilters() }) { Text("Clear all") }
                }
                Spacer(Modifier.height(12.dp))

                Text("Batch", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier  = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.filterBatch.isEmpty(),
                        onClick  = { tutorViewModel.setStudentFilterBatch("") },
                        label    = { Text("All") },
                    )
                    tutorViewModel.allBatches().forEach { batch ->
                        FilterChip(
                            selected = uiState.filterBatch == batch,
                            onClick  = { tutorViewModel.setStudentFilterBatch(batch) },
                            label    = { Text(batch) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Branch", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier  = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.filterBranch.isEmpty(),
                        onClick  = { tutorViewModel.setStudentFilterBranch("") },
                        label    = { Text("All") },
                    )
                    tutorViewModel.allBranches().forEach { branch ->
                        FilterChip(
                            selected = uiState.filterBranch == branch,
                            onClick  = { tutorViewModel.setStudentFilterBranch(branch) },
                            label    = { Text(branch) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun StudentCard(
    student: TutorStudent,
    onClick: () -> Unit,
) {
    val threshold = if (student.isLateralEntry) 40 else 60
    val isPassing = student.totalPoints >= threshold

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                ) {
                    if (student.photoUrl != null) {
                        AsyncImage(
                            model              = student.photoUrl,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    } else {
                        InitialsAvatar(name = student.name, size = 44)
                    }
                }
                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        student.registerNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            "${student.totalPoints} pts",
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (isPassing) {
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Text("Pass", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                student.batch?.name?.let { batch ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(batch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                student.branch?.name?.let { branch ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.School, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(branch, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (student.isLateralEntry) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "Lateral Entry",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR PENDING
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorPendingScreen(tutorViewModel: TutorViewModel) {
    val uiState = tutorViewModel.pendingState.collectAsState().value
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
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(160.dp)) }
                }
            } else if (uiState.certs.isEmpty()) {
                EmptyState(Icons.Outlined.CheckCircle, "All caught up!", "No pending certificates.", Modifier.fillMaxSize())
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
    val context = LocalContext.current

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog  by remember { mutableStateOf(false) }
    // Pre-fill with potential points; tutor can still edit
    var pointsInput  by remember(cert.id) { mutableStateOf(cert.potentialPoints?.toString() ?: "0") }
    var rejectReason by remember(cert.id) { mutableStateOf("") }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            icon    = { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title   = { Text("Approve Certificate") },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Points to award for ${cert.eventName ?: cert.subcategory ?: "this certificate"}:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value         = pointsInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pointsInput = it },
                        label         = { Text("Points") },
                        suffix        = { Text("pts") },
                        singleLine    = true,
                    )
                    cert.potentialPoints?.let {
                        Text(
                            "Suggested: $it pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onApprove(pointsInput.toIntOrNull() ?: 0)
                    showApproveDialog = false
                }) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon    = { Icon(Icons.Outlined.Cancel, null, tint = MaterialTheme.colorScheme.error) },
            title   = { Text("Reject Certificate") },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The student will see this reason.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value         = rejectReason,
                        onValueChange = { rejectReason = it },
                        label         = { Text("Rejection reason *") },
                        minLines      = 3,
                        maxLines      = 5,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = { onReject(rejectReason); showRejectDialog = false },
                    enabled  = rejectReason.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            },
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Student name + reg
            cert.student?.let { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InitialsAvatar(name = s.name, size = 36)
                    Column {
                        Text(s.name, fontWeight = FontWeight.Bold)
                        Text(s.registerNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            // Category / subcategory
            cert.category?.name?.let { Text("$it  ›  ${cert.subcategory ?: ""}", style = MaterialTheme.typography.bodyMedium) }
            cert.eventName?.takeIf { it.isNotBlank() }?.let {
                Text("Event: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!cert.level.isNullOrBlank() || !cert.prizeType.isNullOrBlank()) {
                Text(
                    listOfNotNull(cert.level, cert.prizeType).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Potential points pill
            cert.potentialPoints?.let { pts ->
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Star, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("$pts pts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // View file link
            cert.fileUrl?.let { url ->
                TextButton(
                    onClick  = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View Certificate", fontSize = 13.sp)
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = { showApproveDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
                OutlinedButton(
                    onClick  = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Cancel, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TUTOR APPROVED  — now includes "Revert to Pending" button (mirrors RN app)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TutorApprovedScreen(tutorViewModel: TutorViewModel) {
    val uiState      = tutorViewModel.approvedState.collectAsState().value
    val snackbar     = remember { SnackbarHostState() }
    val context      = LocalContext.current
    var revertTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let {
            snackbar.showSnackbar(it)
            tutorViewModel.clearApprovedActionResult()
        }
    }

    // Confirm revert dialog
    revertTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { revertTarget = null },
            title   = { Text("Revert to Pending?") },
            text    = {
                Text(
                    "This will remove the awarded points and move the certificate back to pending review.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { tutorViewModel.revertCertificate(id); revertTarget = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Yes, Revert") }
            },
            dismissButton = {
                TextButton(onClick = { revertTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh    = { tutorViewModel.loadApproved(isRefresh = true) },
            modifier     = Modifier.padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value         = uiState.searchQuery,
                    onValueChange = tutorViewModel::setApprovedSearch,
                    placeholder   = { Text("Search by student name or reg. number…") },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon  = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { tutorViewModel.setApprovedSearch("") }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth().padding(16.dp),
                    shape      = RoundedCornerShape(12.dp),
                )

                val filtered = tutorViewModel.filteredApproved()

                if (uiState.isLoading) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(90.dp)) }
                    }
                } else if (filtered.isEmpty()) {
                    EmptyState(Icons.Outlined.CheckCircle, "No approved certificates", "", Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { cert ->
                            ApprovedCertCard(
                                cert      = cert,
                                onViewFile = {
                                    cert.fileUrl?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                onRevert  = { revertTarget = cert.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovedCertCard(
    cert: Certificate,
    onViewFile: () -> Unit,
    onRevert: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        cert.eventName ?: cert.subcategory ?: "Certificate",
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    cert.student?.let { s ->
                        Text(
                            "${s.name} · ${s.registerNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    cert.category?.name?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "+${cert.pointsAwarded ?: 0} pts",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cert.fileUrl != null) {
                    OutlinedButton(
                        onClick        = onViewFile,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick        = onRevert,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    Icon(Icons.Outlined.Undo, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Revert", fontSize = 12.sp)
                }
            }
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
            modifier             = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            Icon(Icons.Outlined.TableChart, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Upload Student CSV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Bulk-add students to your batch. They will be assigned to your batch & branch automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            // Required format card
            Card(
                colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Required CSV Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("name", "registerNumber", "email").forEach { col ->
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(col, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    listOf(
                        "First row must be the header exactly as shown",
                        "Register number must be unique per student",
                        "Email must be a valid address",
                    ).forEach { note ->
                        Text("• $note", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick  = { launcher.launch("text/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isUploading,
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick CSV & Upload", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}