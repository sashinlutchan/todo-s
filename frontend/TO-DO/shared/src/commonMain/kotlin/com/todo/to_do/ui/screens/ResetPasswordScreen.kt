package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.todo.to_do.presentation.resetpassword.ResetPasswordSideEffect
import com.todo.to_do.presentation.resetpassword.ResetPasswordViewModel
import com.todo.to_do.ui.components.TaskFlowButtonSpinner
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.theme.TaskFlowTheme
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ResetPasswordScreen(
    resetToken: String,
    onSuccess: () -> Unit,
    viewModel: ResetPasswordViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            ResetPasswordSideEffect.Success -> onSuccess()
            is ResetPasswordSideEffect.ShowError -> toastState.show(effect.message, isError = true)
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
                text = "New password",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = "Choose a strong password",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.submit(resetToken) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) TaskFlowButtonSpinner(modifier = Modifier.size(20.dp))
                else Text("Reset password")
            }
        }
        ToastHost(toastState, modifier = Modifier.padding(padding))
    }
}

