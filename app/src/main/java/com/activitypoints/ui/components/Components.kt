package com.activitypoints.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.activitypoints.ui.theme.ApprovedBg
import com.activitypoints.ui.theme.ApprovedGreen
import com.activitypoints.ui.theme.PendingAmber
import com.activitypoints.ui.theme.PendingBg
import com.activitypoints.ui.theme.RejectedBg
import com.activitypoints.ui.theme.RejectedRed

// ── Status Badge ───────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status.lowercase()) {
        "approved" -> ApprovedBg to ApprovedGreen
        "pending"  -> PendingBg  to PendingAmber
        "rejected" -> RejectedBg to RejectedRed
        else       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier  = modifier,
        color     = bg,
        shape     = RoundedCornerShape(12.dp),
    ) {
        Text(
            text       = status.replaceFirstChar { it.uppercase() },
            color      = fg,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

// ── Avatar initials ────────────────────────────────────────────────────────────

@Composable
fun InitialsAvatar(
    name: String,
    size: Int = 48,
    modifier: Modifier = Modifier,
) {
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Box(
        modifier         = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = initials,
            color      = MaterialTheme.colorScheme.onPrimary,
            fontSize   = (size / 2.5).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── WhatsApp-style Photo Viewer ────────────────────────────────────────────────
//
// Full-screen Dialog with:
//   • Black background
//   • Pinch-to-zoom (0.5x – 5x)
//   • Double-tap to toggle 1x ↔ 2.5x zoom
//   • Pan when zoomed in
//   • Close button (top-right) and tap-on-background-to-close
//   • Smooth spring animation on double-tap

@Composable
fun PhotoViewerDialog(
    photoUrl: String,
    onDismiss: () -> Unit,
) {
    var scale       by remember { mutableFloatStateOf(1f) }
    var offset      by remember { mutableStateOf(Offset.Zero) }
    val animScale   = remember { Animatable(1f) }
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    val scope       = rememberCoroutineScope()

    // Sync animated values → display values
    scale  = animScale.value
    offset = Offset(animOffsetX.value, animOffsetY.value)

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (animScale.value * zoomChange).coerceIn(0.5f, 5f)
        scope.launch { animScale.snapTo(newScale) }
        if (newScale > 1f) {
            scope.launch {
                animOffsetX.snapTo(animOffsetX.value + panChange.x)
                animOffsetY.snapTo(animOffsetY.value + panChange.y)
            }
        } else {
            scope.launch {
                animOffsetX.snapTo(0f)
                animOffsetY.snapTo(0f)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false, // we handle tap ourselves below
        ),
    ) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color.Black)
                // Tap on the black background (outside the image) → dismiss
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                },
            contentAlignment = Alignment.Center,
        ) {
            // ── Zoomable image ─────────────────────────────────────────────────
            AsyncImage(
                model              = photoUrl,
                contentDescription = "Full-size photo",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .graphicsLayer(
                        scaleX        = scale,
                        scaleY        = scale,
                        translationX  = offset.x,
                        translationY  = offset.y,
                    )
                    // Pinch + pan
                    .transformable(state = transformState)
                    // Double-tap: toggle 1x ↔ 2.5x with spring animation
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                scope.launch {
                                    if (animScale.value > 1.2f) {
                                        // Zoom back out to 1x, center
                                        animScale.animateTo(1f,   animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                        animOffsetX.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                        animOffsetY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                    } else {
                                        // Zoom in 2.5x centered on tap point
                                        animScale.animateTo(2.5f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                        // Shift so the tapped point stays centered
                                        val screenCx = size.width  / 2f
                                        val screenCy = size.height / 2f
                                        animOffsetX.animateTo((screenCx - tapOffset.x) * 1.5f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                        animOffsetY.animateTo((screenCy - tapOffset.y) * 1.5f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                        )
                    },
            )

            // ── Close button (top-right) ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint               = Color.White,
                    )
                }
            }
        }
    }
}

// ── Shimmer skeleton ───────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_x",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = shimmerColors,
                    start  = Offset(translateAnim - 500, 0f),
                    end    = Offset(translateAnim, 0f),
                )
            ),
    )
}

// ── Empty state ────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = title,
            style     = MaterialTheme.typography.titleMedium,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Error state ────────────────────────────────────────────────────────────────

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text  = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

// ── Points progress bar ────────────────────────────────────────────────────────

@Composable
fun PointsProgressBar(
    current: Int,
    required: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = (current.toFloat() / required.toFloat()).coerceIn(0f, 1f)
    val passed   = current >= required
    val barColor = if (passed) ApprovedGreen else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "$current pts",
                style      = MaterialTheme.typography.labelMedium,
                color      = barColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = "Required: $required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color      = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
