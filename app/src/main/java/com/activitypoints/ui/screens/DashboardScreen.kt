package com.activitypoints.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.activitypoints.models.Certificate
import com.activitypoints.ui.components.*
import com.activitypoints.ui.theme.TrophyGold
import com.activitypoints.viewmodel.AuthState
import com.activitypoints.viewmodel.AuthViewModel
import com.activitypoints.viewmodel.StudentViewModel

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    studentViewModel: StudentViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToCerts: () -> Unit,
) {
    val authState    by authViewModel.authState.collectAsState()
    val uiState      by studentViewModel.uiState.collectAsState()

    val studentProfile = (authState as? AuthState.Student)?.profile
    val displayName    = studentProfile?.name ?: "Student"
    val photoUrl       = studentProfile?.photoUrl

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh    = { studentViewModel.loadAll(isRefresh = true) },
    ) {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            item(key = "header") {
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment    = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text  = "Hello, ${displayName.split(" ").firstOrNull() ?: "Student"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text  = "Activity Points Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Avatar - tappable → profile
                    IconButton(onClick = onNavigateToProfile) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model  = photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.size(40.dp),
                            )
                        } else {
                            InitialsAvatar(name = displayName, size = 40)
                        }
                    }
                }
            }

            // ── Points card ────────────────────────────────────────────────────
            item(key = "points_card") {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint   = TrophyGold,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text  = "Total Activity Points",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text       = "${uiState.totalPoints}",
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        // Lateral entry toggle
                        Row(
                            modifier  = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = "Lateral Entry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                            Switch(
                                checked         = uiState.isLateralEntry,
                                onCheckedChange = { studentViewModel.setLateralEntry(it) },
                                colors          = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                ),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        PointsProgressBar(
                            current  = uiState.totalPoints,
                            required = uiState.passThreshold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Summary chips ──────────────────────────────────────────────────
            item(key = "summary") {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val certs    = uiState.certificates
                    val approved = certs.count { it.status.lowercase() == "approved" }
                    val pending  = certs.count { it.status.lowercase() == "pending" }
                    val rejected = certs.count { it.status.lowercase() == "rejected" }

                    SummaryChip("Approved", approved, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    SummaryChip("Pending",  pending,  MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    SummaryChip("Rejected", rejected, MaterialTheme.colorScheme.error,    Modifier.weight(1f))
                }
            }

            // ── Recent activity header ─────────────────────────────────────────
            item(key = "recent_header") {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToCerts) { Text("See all") }
                }
            }

            // ── Loading skeleton ───────────────────────────────────────────────
            if (uiState.isLoading) {
                items(3) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(72.dp))
                }
            } else if (uiState.error != null) {
                item {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { studentViewModel.loadAll() },
                    )
                }
            } else if (uiState.certificates.isEmpty()) {
                item {
                    EmptyState(
                        icon     = Icons.Outlined.Inbox,
                        title    = "No certificates yet",
                        subtitle = "Upload your activity certificates to get started.",
                    )
                }
            } else {
                // Show latest 5 certs
                items(
                    items = uiState.certificates.take(5),
                    key   = { it.id },
                ) { cert ->
                    ActivityRow(cert)
                }
            }
        }
    }
}

// ── Small summary chip ─────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(label: String, count: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text       = "$count",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = color,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Activity row ───────────────────────────────────────────────────────────────

@Composable
private fun ActivityRow(cert: Certificate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text       = cert.eventName ?: cert.subcategory ?: "Certificate",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                )
                cert.category?.name?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(cert.status)
                val pts = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints
                if (pts != null) {
                    Text(
                        text  = "$pts pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
