package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todo.to_do.presentation.resetcode.VerifyResetCodeSideEffect
import com.todo.to_do.presentation.resetcode.VerifyResetCodeViewModel
import com.todo.to_do.ui.components.OtpInputField
import com.todo.to_do.ui.components.TaskFlowButtonSpinner
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.theme.TaskFlowTheme
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun VerifyResetCodeScreen(
    email: String,
    onVerified: (resetToken: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: VerifyResetCodeViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is VerifyResetCodeSideEffect.Verified -> onVerified(effect.resetToken)
            is VerifyResetCodeSideEffect.ShowError -> toastState.show(effect.message, isError = true)
            is VerifyResetCodeSideEffect.CodeResent -> toastState.show(effect.message)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colors.surfacePage)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter reset code",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = "We texted a 6-digit code to the phone number on your account",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            OtpInputField(
                value = state.code,
                onValueChange = viewModel::onCodeChange
            )

            LaunchedEffect(state.code) {
                if (state.code.length == 6 && !state.isLoading) {
                    viewModel.verifyCode(email)
                }
            }

            if (state.isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = colors.accent600,
                    modifier = Modifier.padding(16.dp)
                )
            }

            TextButton(onClick = { viewModel.resendCode(email) }, enabled = !state.isLoading) {
                Text("Resend code", color = colors.accent400)
            }
            TextButton(onClick = onNavigateBack, enabled = !state.isLoading) {
                Text("Back", color = colors.textSecondary)
            }
        }
        ToastHost(toastState, modifier = Modifier.padding(padding))
    }
}
