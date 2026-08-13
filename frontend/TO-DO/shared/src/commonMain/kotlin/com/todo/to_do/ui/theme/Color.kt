package com.todo.to_do.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TaskFlowColors(
    val surfacePage: Color,
    val surfaceCard: Color,
    val surfaceMuted: Color,
    val accent50: Color,
    val accent400: Color,
    val accent600: Color,
    val accent800: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val danger: Color,
    val warning: Color
)

val LightTaskFlowColors = TaskFlowColors(
    surfacePage = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF4F6F2),
    accent50 = Color(0xFFEAF3DE),
    accent400 = Color(0xFF639922),
    accent600 = Color(0xFF3B6D11),
    accent800 = Color(0xFF27500A),
    textPrimary = Color(0xFF1A1D18),
    textSecondary = Color(0xFF6B6F66),
    textMuted = Color(0xFF9B9E96),
    border = Color(0xFFE5E8E1),
    danger = Color(0xFFE24B4A),
    warning = Color(0xFFEF9F27)
)

val DarkTaskFlowColors = TaskFlowColors(
    surfacePage = Color(0xFF12140F),
    surfaceCard = Color(0xFF1B1E17),
    surfaceMuted = Color(0xFF22251D),
    accent50 = Color(0xFF1F2E12),
    accent400 = Color(0xFF8FCB4E),
    accent600 = Color(0xFF97C459),
    accent800 = Color(0xFFC0DD97),
    textPrimary = Color(0xFFF1F3EE),
    textSecondary = Color(0xFF9CA096),
    textMuted = Color(0xFF6B6F66),
    border = Color(0xFF2A2D24),
    danger = Color(0xFFF09595),
    warning = Color(0xFFEF9F27)
)

