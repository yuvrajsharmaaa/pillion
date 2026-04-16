package com.pillion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0A6E5A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF9FF2DE),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF002019),
    secondary = androidx.compose.ui.graphics.Color(0xFF0061A4),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD2E4FF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF001D36),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6A5778),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF1DBFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF241430),
    background = androidx.compose.ui.graphics.Color(0xFFF4FAF8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF161D1B),
    surface = androidx.compose.ui.graphics.Color(0xFFFCFDFB),
    onSurface = androidx.compose.ui.graphics.Color(0xFF161D1B),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDBE5E0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF3F4945),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF82D6C3),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00382C),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF005141),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF9FF2DE),
    secondary = androidx.compose.ui.graphics.Color(0xFFA1C9FF),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF00315B),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF00497F),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD2E4FF),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD7BEE7),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3A2947),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF51405F),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF1DBFF),
    background = androidx.compose.ui.graphics.Color(0xFF0F1513),
    onBackground = androidx.compose.ui.graphics.Color(0xFFDEE4E1),
    surface = androidx.compose.ui.graphics.Color(0xFF101715),
    onSurface = androidx.compose.ui.graphics.Color(0xFFDEE4E1),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3F4945),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBEC9C4),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
)

@Composable
fun PillionTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
