package com.doginventory.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class SemanticColors(
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val warning: Color,
    val warningAccent: Color,
    val warningContainer: Color,
    val warningOnContainer: Color,
    val inventoryExpired: Color,
    val inventoryExpiredContainer: Color,
    val completed: Color,
    val completedText: Color,
    val muted: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        danger = Color.Unspecified,
        onDanger = Color.Unspecified,
        dangerContainer = Color.Unspecified,
        warning = Color.Unspecified,
        warningAccent = Color.Unspecified,
        warningContainer = Color.Unspecified,
        warningOnContainer = Color.Unspecified,
        inventoryExpired = Color.Unspecified,
        inventoryExpiredContainer = Color.Unspecified,
        completed = Color.Unspecified,
        completedText = Color.Unspecified,
        muted = Color.Unspecified
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    tertiary = DarkAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = DarkBackground,
    onPrimaryContainer = DarkOnSurface,
    onSecondary = DarkBackground,
    onSecondaryContainer = DarkOnSurface,
    onTertiary = DarkBackground,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    outline = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    tertiary = LightAccent,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = White,
    onPrimaryContainer = LightOnSurface,
    onSecondary = LightOnSurface,
    onSecondaryContainer = LightOnSurface,
    onTertiary = White,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    outline = LightOutline,
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

private val DarkSemanticColors = SemanticColors(
    danger = DarkDanger,
    onDanger = DarkBackground,
    dangerContainer = DarkDangerContainer,
    warning = DarkWarning,
    warningAccent = DarkWarningAccent,
    warningContainer = DarkWarningContainer,
    warningOnContainer = DarkWarningOnContainer,
    inventoryExpired = DarkExpired,
    inventoryExpiredContainer = DarkExpiredContainer,
    completed = DarkDone,
    completedText = DarkHint,
    muted = DarkHint
)

private val LightSemanticColors = SemanticColors(
    danger = LightDanger,
    onDanger = White,
    dangerContainer = LightDangerContainer,
    warning = LightWarning,
    warningAccent = LightWarningAccent,
    warningContainer = LightWarningContainer,
    warningOnContainer = LightWarningOnContainer,
    inventoryExpired = LightExpired,
    inventoryExpiredContainer = LightExpiredContainer,
    completed = LightDone,
    completedText = LightHint,
    muted = LightHint
)

@Composable
fun DogInventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

@Composable
fun SystemBarsStyle(
    navigationBarColor: Color,
    statusBarColor: Color = MaterialTheme.colorScheme.background,
    darkTheme: Boolean = statusBarColor.luminance() < 0.5f
) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

object DogInventoryTheme {
    val semanticColors: SemanticColors
        @Composable
        get() = LocalSemanticColors.current
}
