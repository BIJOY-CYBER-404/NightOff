package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NightDarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    tertiary = NightTertiary,
    onTertiary = NightOnTertiary,
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    error = NightError,
    onError = Color(0xFF4C0519)
)

@Composable
fun NightOffTheme(
    content: @Composable () -> Unit
) {
    // NightOff is deliberately dark-first for bedtime comfort and AMOLED battery savings
    MaterialTheme(
        colorScheme = NightDarkColorScheme,
        typography = Typography,
        content = content
    )
}
