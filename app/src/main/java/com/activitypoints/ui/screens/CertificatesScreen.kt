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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.activitypoints.models.Certificate
import com.activitypoints.ui.components.*
import com.activitypoints.viewmodel.StudentViewModel

private val FILTERS = listOf("all", "approved", "pending", "rejected")

@Composable
fun CertificatesScreen(
    studentViewModel: StudentViewModel,
) {
    val uiState by studentViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var deletingId by remember { mutableStateOf<String?>(null) }
    var certToDelete by remember { mutableStateOf<Certificate?>(null) }

    // Confirm delete dialog
    if (certToDelete != null) {
        AlertDialog(
            onDismissRequest = { certToDelete = null },
            title = { Text("Delete Certificate?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        certToDelete?.let {
                            deletingId = it.id
                            studentViewModel.deleteCertificate(it.id)
                        }
                        certToDelete = null
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { certToDelete = null }) { Text("Cancel") }
            },
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh    = { studentViewModel.loadAll(isRefresh = true) },
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Filter chips ───────────────────────────────────────────────────
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FILTERS) { filter ->
                    FilterChip(
                        selected = uiState.activeFilter == filter,
                        onClick  = { studentViewModel.setFilter(filter) },
                        label    = { Text(filter.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Content ────────────────────────────────────────────────────────
            if (uiState.isLoading) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(5) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            } else if (uiState.error != null) {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { studentViewModel.loadAll() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val filtered = studentViewModel.getFilteredCertificates()
                if (filtered.isEmpty()) {
                    EmptyState(
                        icon     = Icons.Outlined.WorkspacePremium,
                        title    = "No certificates",
                        subtitle = "No ${uiState.activeFilter} certificates found.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { cert ->
                            CertificateCard(
                                cert        = cert,
                                isDeleting  = deletingId == cert.id,
                                onDelete    = { certToDelete = cert },
                                onOpenFile  = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Certificate card ───────────────────────────────────────────────────────────

@Composable
private fun CertificateCard(
    cert: Certificate,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val displayPoints = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.Top,
            ) {
                Text(
                    text       = cert.eventName ?: cert.subcategory ?: "Certificate",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 2,
                )
                StatusBadge(cert.status)
            }

            cert.category?.name?.let { cat ->
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label   = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                )
            }

            if (cert.level != null || cert.prizeType != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MilitaryTech, null, Modifier.size(16.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = listOfNotNull(cert.level, cert.prizeType).joinToString(" — "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (displayPoints != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "$displayPoints pts",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (cert.rejectionReason != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Reason: ${cert.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                cert.fileUrl?.let { url ->
                    TextButton(onClick = { onOpenFile(url) }) {
                        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View File")
                    }
                }
                if (cert.status.lowercase() == "pending") {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isDeleting,
                        colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        if (isDeleting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Delete") }
                    }
                }
            }
        }
    }
}
