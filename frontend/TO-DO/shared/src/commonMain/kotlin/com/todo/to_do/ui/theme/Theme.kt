package com.todo.to_do.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTaskFlowColors = staticCompositionLocalOf { LightTaskFlowColors }

object TaskFlowTheme {
    val colors: TaskFlowColors
        @Composable get() = LocalTaskFlowColors.current
}

@Composable
fun TaskFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTaskFlowColors else LightTaskFlowColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = tokens.accent600,
            onPrimary = tokens.surfacePage,
            background = tokens.surfacePage,
            onBackground = tokens.textPrimary,
            surface = tokens.surfaceCard,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceMuted,
            error = tokens.danger,
            outline = tokens.border
        )
    } else {
        lightColorScheme(
            primary = tokens.accent600,
            onPrimary = tokens.surfacePage,
            background = tokens.surfacePage,
            onBackground = tokens.textPrimary,
            surface = tokens.surfaceCard,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceMuted,
            error = tokens.danger,
            outline = tokens.border
        )
    }

    CompositionLocalProvider(LocalTaskFlowColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaskFlowTypography,
            shapes = TaskFlowShapes,
            content = content
        )
    }
}

