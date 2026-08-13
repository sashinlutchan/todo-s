package com.todo.to_do.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todo.to_do.ui.theme.TaskFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ToastMessage(val text: String, val isError: Boolean)

class ToastState(private val scope: CoroutineScope) {
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    private var dismissJob: Job? = null

    fun show(text: String, isError: Boolean = false) {
        dismissJob?.cancel()
        current = ToastMessage(text, isError)
        dismissJob = scope.launch {
            delay(4500)
            current = null
        }
    }
}

@Composable
fun rememberToastState(): ToastState {
    val scope = rememberCoroutineScope()
    return remember { ToastState(scope) }
}

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val colors = TaskFlowTheme.colors
    val message = state.current

    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 }
        ) {
            val accent = if (message?.isError == true) colors.danger else colors.accent600
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceCard)
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(accent)
                )
                Text(
                    text = message?.text.orEmpty(),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

