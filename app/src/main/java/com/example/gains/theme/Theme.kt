package com.example.gains.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = InfraredAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1F17),
    onPrimaryContainer = Color(0xFFFF9E80),
    background = Color(0xFF121214),
    onBackground = Color.White,
    surface = Color(0xFF1E1E24),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A32),
    onSurfaceVariant = Color(0xFFE4E4E6),
    outline = Color(0xFF3E3E48),
    secondary = TextSecondary,
    onSecondary = Color.White,
    error = SystemRed
)

private val LightColorScheme = lightColorScheme(
    primary = InfraredAccent,
    onPrimary = Color.White,
    primaryContainer = PrimarySoftBg,
    onPrimaryContainer = InfraredAccent,
    background = SystemBackground,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = SystemGrayLight,
    onSurfaceVariant = TextPrimary,
    outline = BorderColor,
    secondary = TextSecondary,
    onSecondary = Color.White,
    error = SystemRed
)

@Composable
fun GAINSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default dynamicColor to false to ensure our custom premium styling is consistently used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
