package com.todo.to_do.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todo.to_do.ui.theme.TaskFlowTheme

@Composable
fun TaskFlowTopLoader(visible: Boolean, modifier: Modifier = Modifier) {
    val colors = TaskFlowTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = colors.accent600,
            trackColor = colors.accent50
        )
    }
}

@Composable
fun TaskFlowLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val colors = TaskFlowTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfacePage.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = colors.accent600, trackColor = colors.accent50)
                if (label != null) {
                    Text(text = label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun TaskFlowButtonSpinner(modifier: Modifier = Modifier) {
    val colors = TaskFlowTheme.colors
    CircularProgressIndicator(
        color = colors.surfacePage,
        trackColor = colors.accent400,
        strokeWidth = 2.dp,
        modifier = modifier
    )
}
