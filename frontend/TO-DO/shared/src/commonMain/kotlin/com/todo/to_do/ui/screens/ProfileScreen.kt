package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todo.to_do.presentation.profile.ProfileSideEffect
import com.todo.to_do.presentation.profile.ProfileViewModel
import com.todo.to_do.ui.components.TaskFlowTopLoader
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.theme.TaskFlowTheme
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is ProfileSideEffect.ShowError -> toastState.show(effect.message, isError = true)
            ProfileSideEffect.LoggedOut -> onLoggedOut()
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colors.surfacePage)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Profile", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                TextButton(onClick = onBack) { Text("Back", color = colors.accent400) }
            }
            TaskFlowTopLoader(visible = state.isLoading)

            ProfileField(label = "Display name", value = state.displayName)
            ProfileField(label = "Email", value = state.email)
            ProfileField(label = "Member since", value = state.createdAt)

            OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                Text("Log out", color = colors.danger)
            }
        }
        ToastHost(toastState, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    val colors = TaskFlowTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Text(text = value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}

