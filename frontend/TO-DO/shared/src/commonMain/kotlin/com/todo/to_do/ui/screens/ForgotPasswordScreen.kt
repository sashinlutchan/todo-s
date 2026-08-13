package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todo.to_do.presentation.forgotpassword.ForgotPasswordSideEffect
import com.todo.to_do.presentation.forgotpassword.ForgotPasswordViewModel
import com.todo.to_do.ui.components.TaskFlowButtonSpinner
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.theme.TaskFlowTheme
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ForgotPasswordScreen(
    onCodeSent: (email: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is ForgotPasswordSideEffect.CodeSent -> onCodeSent(effect.email)
            is ForgotPasswordSideEffect.ShowError -> toastState.show(effect.message, isError = true)
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
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNavigateBack) { Text("Back", color = colors.accent400) }
            }

            Text(
                text = "Forgot password",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = "Enter your email and we'll text a reset code to the phone number on your account",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading && state.email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) TaskFlowButtonSpinner(modifier = Modifier.size(20.dp))
                else Text("Send reset code")
            }
        }
        ToastHost(toastState, modifier = Modifier.padding(padding))
    }
}

