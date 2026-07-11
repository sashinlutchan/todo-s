package com.todo.to_do.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.todo.to_do.data.remote.SessionStore
import com.todo.to_do.ui.theme.TaskFlowTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
    onFinished: (isLoggedIn: Boolean) -> Unit,
    sessionStore: SessionStore = koinInject()
) {
    val colors = TaskFlowTheme.colors

    var logoScale by remember { mutableFloatStateOf(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val loaderAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.snapTo(0f)
        logoAlpha.animateTo(1f, tween(durationMillis = 350))
    }

    LaunchedEffect(Unit) {
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = EaseOutBack)
        ) { logoScale = value }
    }

    LaunchedEffect(Unit) {
        delay(200)
        textAlpha.animateTo(1f, tween(durationMillis = 400))
    }

    LaunchedEffect(Unit) {
        delay(500)
        loaderAlpha.animateTo(1f, tween(durationMillis = 300))
    }

    LaunchedEffect(Unit) {
        val isLoggedIn = sessionStore.session.value != null
        delay(1400)
        onFinished(isLoggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colors.surfacePage),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.accent600.copy(alpha = logoAlpha.value)),
                contentAlignment = Alignment.Center
            ) {
                CheckMark(color = colors.surfacePage, modifier = Modifier.size(44.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "TO-DO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary.copy(alpha = textAlpha.value)
                )
                Text(
                    text = "Organize your day, effortlessly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary.copy(alpha = textAlpha.value)
                )
            }
        }

        CircularProgressIndicator(
            color = colors.accent600.copy(alpha = loaderAlpha.value),
            trackColor = colors.accent50.copy(alpha = loaderAlpha.value),
            strokeWidth = 3.dp,
            modifier = Modifier
                .padding(bottom = 56.dp)
                .align(Alignment.BottomCenter)
                .size(28.dp)
        )
    }
}

@Composable
private fun CheckMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.76f)
            lineTo(size.width * 0.84f, size.height * 0.26f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}