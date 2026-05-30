package com.activitypoints.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val authState by authViewModel.authState.collectAsState()
    val uiState   by studentViewModel.uiState.collectAsState()

    // Always use the latest profile from authState (refreshed by loadAll)
    val studentProfile = (authState as? AuthState.Student)?.profile
    val displayName    = studentProfile?.name?.takeIf { it.isNotBlank() } ?: "Student"
    val firstName      = displayName.split(" ").firstOrNull() ?: displayName
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
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Avatar + greeting (left side, like RN app)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Tappable avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onNavigateToProfile() },
                        ) {
                            if (photoUrl != null) {
                                AsyncImage(
                                    model              = photoUrl,
                                    contentDescription = "Profile photo",
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize(),
                                )
                            } else {
                                InitialsAvatar(name = displayName, size = 48)
                            }
                        }

                        // Greeting text
                        Column {
                            Text(
                                text       = "Hello, $firstName",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text  = "Welcome back!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 3-dot menu (right side)
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        DropdownMenu(
                            expanded        = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Profile") },
                                leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                                onClick     = { menuExpanded = false; onNavigateToProfile() },
                            )
                            DropdownMenuItem(
                                text        = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick     = { menuExpanded = false; authViewModel.logout() },
                            )
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
                    shape     = RoundedCornerShape(22.dp),
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text  = "Activity Points",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color    = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Text(
                                    text       = "${uiState.totalPoints}",
                                    fontSize   = 52.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Text(
                                text  = "of ${uiState.passThreshold} required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(14.dp))
                            PointsProgressBar(
                                current  = uiState.totalPoints,
                                required = uiState.passThreshold,
                                modifier = Modifier.width(200.dp),
                            )
                        }
                        Icon(
                            Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint     = TrophyGold,
                            modifier = Modifier.size(50.dp),
                        )
                    }
                }
            }

            // ── Passed banner ──────────────────────────────────────────────────
            if (!uiState.isLoading && uiState.totalPoints >= uiState.passThreshold) {
                item(key = "pass_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Activity Points Completed!",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "You have met the required activity points.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(
                                    "PASS",
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 11.sp,
                                )
                            }
                        }
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

                    SummaryChip("Approved", approved, MaterialTheme.colorScheme.primary,  Modifier.weight(1f))
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
                    Text(
                        "Recent Activity",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(cert.status)
                val pts = if (cert.status.lowercase() == "approved") cert.pointsAwarded else cert.potentialPoints
                if (pts != null) {
                    Text(
                        text       = "+$pts pts",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
