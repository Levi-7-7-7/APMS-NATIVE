package com.activitypoints.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.activitypoints.models.Certificate
import com.activitypoints.ui.components.*
import com.activitypoints.ui.theme.ApprovedBg
import com.activitypoints.ui.theme.ApprovedGreen
import com.activitypoints.ui.theme.PendingAmber
import com.activitypoints.ui.theme.PendingBg
import com.activitypoints.ui.theme.RejectedBg
import com.activitypoints.ui.theme.RejectedRed
import com.activitypoints.viewmodel.StudentViewModel
import java.text.SimpleDateFormat
import java.util.*

private val FILTERS = listOf("all", "approved", "pending", "rejected")
private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
fun CertificatesScreen(
    studentViewModel: StudentViewModel,
) {
    val uiState by studentViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var deletingId   by remember { mutableStateOf<String?>(null) }
    var certToDelete by remember { mutableStateOf<Certificate?>(null) }

    if (certToDelete != null) {
        AlertDialog(
            onDismissRequest = { certToDelete = null },
            title = { Text("Cancel Certificate?") },
            text  = {
                Text("Cancel and delete \"${certToDelete?.eventName ?: certToDelete?.subcategory ?: "this certificate"}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        certToDelete?.let { deletingId = it.id; studentViewModel.deleteCertificate(it.id) }
                        certToDelete = null
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { certToDelete = null }) { Text("Cancel") } },
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh    = { studentViewModel.loadAll(isRefresh = true) },
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Total points summary strip ─────────────────────────────────────
            if (!uiState.isLoading && uiState.certificates.isNotEmpty()) {
                Surface(
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Activity Points",
                            style  = MaterialTheme.typography.labelMedium,
                            color  = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${uiState.totalPoints} / ${uiState.passThreshold} pts",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // ── Filter chips ───────────────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FILTERS) { filter ->
                    val count = when (filter) {
                        "all"      -> uiState.certificates.size
                        else       -> uiState.certificates.count { it.status.lowercase() == filter }
                    }
                    FilterChip(
                        selected = uiState.activeFilter == filter,
                        onClick  = { studentViewModel.setFilter(filter) },
                        label    = {
                            Text("${filter.replaceFirstChar { it.uppercase() }} ($count)")
                        },
                    )
                }
            }

            // ── Content ────────────────────────────────────────────────────────
            if (uiState.isLoading) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(4) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp)) }
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
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { cert ->
                            CertificateCard(
                                cert       = cert,
                                isDeleting = deletingId == cert.id,
                                onDelete   = { certToDelete = cert },
                                onOpenFile = { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Certificate card (mirrors RN CertCard) ─────────────────────────────────────

@Composable
private fun CertificateCard(
    cert: Certificate,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val displayPoints = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints

    val (statusBg, statusFg) = when (cert.status.lowercase()) {
        "approved" -> ApprovedBg to ApprovedGreen
        "pending"  -> PendingBg  to PendingAmber
        "rejected" -> RejectedBg to RejectedRed
        else       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when (cert.status.lowercase()) {
        "approved" -> ApprovedGreen.copy(alpha = 0.4f)
        "rejected" -> RejectedRed.copy(alpha = 0.4f)
        else       -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Top row: name + status icon ──────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                Text(
                    text       = cert.eventName ?: cert.subcategory ?: "Certificate",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines   = 2,
                )
                val statusIcon = when (cert.status.lowercase()) {
                    "approved" -> Icons.Outlined.CheckCircle
                    "pending"  -> Icons.Outlined.AccessTime
                    else       -> Icons.Outlined.Cancel
                }
                Icon(statusIcon, null, Modifier.size(22.dp), tint = statusFg)
            }

            // ── Category badge ────────────────────────────────────────────────
            cert.category?.name?.let { cat ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text     = cat,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── Level / prize ─────────────────────────────────────────────────
            if (cert.level != null || cert.prizeType != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.MilitaryTech, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = listOfNotNull(cert.level, cert.prizeType).joinToString(" — "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Status badge ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text     = cert.status.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = statusFg,
                )
            }

            // ── Footer: date + points ─────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val dateStr = cert.createdAt?.let { dateValue ->
                        try {
                            val isoFormat = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.getDefault()
                            )

                            isoFormat.timeZone = TimeZone.getTimeZone("UTC")

                            val parsedDate = isoFormat.parse(dateValue)

                            if (parsedDate != null) {
                                DATE_FORMAT.format(parsedDate)
                            } else {
                                dateValue
                            }
                        } catch (_: Exception) {
                            try {
                                val isoFormat = SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    Locale.getDefault()
                                )

                                isoFormat.timeZone = TimeZone.getTimeZone("UTC")

                                val parsedDate = isoFormat.parse(dateValue)

                                if (parsedDate != null) {
                                    DATE_FORMAT.format(parsedDate)
                                } else {
                                    dateValue
                                }
                            } catch (_: Exception) {
                                dateValue
                            }
                        }
                    } ?: "—"

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (displayPoints != null) {
                    Text(
                        text = "+$displayPoints pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = ApprovedGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Rejection reason box (mirrors RN rejectedBox) ──────────────────
            if (cert.status.lowercase() == "rejected") {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color    = Color(0xFFFFF0F0),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Cancel, null, Modifier.size(16.dp), tint = RejectedRed)
                            Text(
                                text       = "Certificate Rejected",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color      = RejectedRed,
                            )
                        }
                        Text(
                            text  = cert.rejectionReason ?: "No reason provided. Please contact your tutor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D),
                        )
                        Text(
                            text  = "You can re-upload a corrected certificate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Action buttons ─────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                cert.fileUrl?.let { url ->
                    TextButton(
                        onClick = { onOpenFile(url) },
                        colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open File", fontSize = 12.sp)
                    }
                }
                if (cert.status.lowercase() == "pending") {
                    TextButton(
                        onClick  = onDelete,
                        enabled  = !isDeleting,
                        colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
