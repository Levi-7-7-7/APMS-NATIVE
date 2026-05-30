package com.activitypoints.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
    var viewingCert by remember { mutableStateOf<Certificate?>(null) }

    LaunchedEffect(studentId) {
        tutorViewModel.loadStudentDetail(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text(uiState.student?.name ?: "Student Details") },
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

        val student   = uiState.student
        val certs     = tutorViewModel.filteredDetailCerts()
        val approved  = uiState.certificates.filter { it.status.lowercase() == "approved" }

        // ── Computed stats — categories aware, matching RN exactly ──────────
        val cappedTotal  = tutorViewModel.detailCappedPoints()
        val rawTotal     = tutorViewModel.detailRawPoints()
        val threshold    = CalcPoints.passThreshold(student?.isLateralEntry ?: false)
        val hasPassed    = cappedTotal >= threshold
        val ptsLeft      = threshold - cappedTotal

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Profile card ─────────────────────────────────────────────────
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = student?.name ?: "?", size = 56)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    student?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    student?.registerNumber ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                student?.email?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (student?.isLateralEntry == true) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text(
                                            "Lateral Entry",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))

                        // ── Raw | Capped | Pass/Fail stats row ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            // Raw Total
                            StatBox(
                                value = "$rawTotal",
                                label = "Raw Total",
                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            VerticalDivider(modifier = Modifier.height(56.dp))
                            // Capped Total
                            StatBox(
                                value = "$cappedTotal",
                                label = "Capped Total",
                                valueColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            VerticalDivider(modifier = Modifier.height(56.dp))
                            // Pass / Fail
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hasPassed) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = if (hasPassed) Color(0xFF16a34a) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (hasPassed) "Pass" else "${ptsLeft} left",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasPassed) Color(0xFF16a34a) else MaterialTheme.colorScheme.error,
                                    )
                                }
                                Text(
                                    text  = "$threshold pts req.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Filter chips ─────────────────────────────────────────────────
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

            // ── Certificates ─────────────────────────────────────────────────
            if (certs.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Outlined.WorkspacePremium,
                        "No certificates",
                        "No ${uiState.activeFilter} certificates.",
                        Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(certs, key = { it.id }) { cert ->
                    DetailCertCard(cert, onView = { viewingCert = cert })
                }
            }
        }
    }

    // ── Certificate image/file viewer ─────────────────────────────────────────
    viewingCert?.let { cert ->
        CertViewerDialog(cert = cert, onDismiss = { viewingCert = null })
    }
}

// ── Stat box helper ────────────────────────────────────────────────────────────

@Composable
private fun StatBox(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier             = modifier.padding(vertical = 8.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Certificate card with View button ─────────────────────────────────────────

@Composable
private fun DetailCertCard(cert: Certificate, onView: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        cert.eventName ?: cert.subcategory ?: "Certificate",
                        fontWeight = FontWeight.SemiBold,
                    )
                    cert.category?.name?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val pts = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints
                    if (pts != null) {
                        Text("$pts pts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                StatusBadge(cert.status)
            }
            cert.rejectionReason?.let { reason ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Outlined.Cancel, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Rejected: $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            if (!cert.fileUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = onView,
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Outlined.Visibility, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Certificate file viewer dialog ────────────────────────────────────────────

@Composable
private fun CertViewerDialog(cert: Certificate, onDismiss: () -> Unit) {
    val context  = LocalContext.current
    val url      = cert.fileUrl ?: ""
    val isPdf    = url.lowercase().let { it.contains(".pdf") || it.contains("pdf") }
    val fileName = "${cert.eventName ?: cert.subcategory ?: "Certificate"}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                // Toolbar
                TopAppBar(
                    title = {
                        Text(
                            fileName,
                            style    = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.ArrowBack, "Close")
                        }
                    },
                    actions = {
                        if (url.isNotBlank()) {
                            IconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) {
                                Icon(Icons.Outlined.OpenInNew, "Open externally")
                            }
                        }
                    },
                )

                if (url.isBlank()) {
                    // No file
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.InsertDriveFile, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            Text("No file attached", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (isPdf) {
                    // PDF — open externally
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(Modifier.padding(32.dp)) {
                            Column(
                                Modifier.padding(32.dp),
                                horizontalAlignment  = Alignment.CenterHorizontally,
                                verticalArrangement  = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(64.dp), tint = Color(0xFFef4444))
                                Text("PDF Certificate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }) {
                                    Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open in PDF Viewer")
                                }
                            }
                        }
                    }
                } else {
                    // Image — show inline with Coil
                    var isLoading by remember { mutableStateOf(true) }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model             = url,
                            contentDescription = "Certificate image",
                            modifier          = Modifier.fillMaxSize(),
                            onSuccess         = { isLoading = false },
                            onError           = { isLoading = false },
                        )
                        if (isLoading) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
