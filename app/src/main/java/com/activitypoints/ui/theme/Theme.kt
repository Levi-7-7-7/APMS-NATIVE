package com.activitypoints.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Brand colours (mirrored from RN theme/index.ts) ───────────────────────────

// Light
val LightPrimary        = Color(0xFF1E3A8A)
val LightOnPrimary      = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFBFDBFE)
val LightBackground     = Color(0xFFF0F4FF)
val LightSurface        = Color(0xFFFFFFFF)
val LightOnBackground   = Color(0xFF111827)
val LightOnSurface      = Color(0xFF111827)
val LightOutline        = Color(0xFFE5E7EB)

// Dark
val DarkPrimary         = Color(0xFF60A5FA)
val DarkOnPrimary       = Color(0xFF1E3A8A)
val DarkPrimaryContainer = Color(0xFF1E3A5F)
val DarkBackground      = Color(0xFF0F172A)
val DarkSurface         = Color(0xFF1E293B)
val DarkOnBackground    = Color(0xFFF1F5F9)
val DarkOnSurface       = Color(0xFFF1F5F9)
val DarkOutline         = Color(0xFF334155)

// Status colours (used throughout the app)
val ApprovedGreen       = Color(0xFF059669)
val ApprovedBg          = Color(0xFFD1FAE5)
val PendingAmber        = Color(0xFFD97706)
val PendingBg           = Color(0xFFFEF9C3)
val RejectedRed         = Color(0xFFDC2626)
val RejectedBg          = Color(0xFFFEE2E2)
val TrophyGold          = Color(0xFFFBBF24)

private val LightColors = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    background       = LightBackground,
    surface          = LightSurface,
    onBackground     = LightOnBackground,
    onSurface        = LightOnSurface,
    outline          = LightOutline,
)

private val DarkColors = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    background       = DarkBackground,
    surface          = DarkSurface,
    onBackground     = DarkOnBackground,
    onSurface        = DarkOnSurface,
    outline          = DarkOutline,
)

@Composable
fun ActivityPointsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
