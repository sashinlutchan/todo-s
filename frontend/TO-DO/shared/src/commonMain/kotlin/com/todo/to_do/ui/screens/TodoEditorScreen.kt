package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.presentation.editor.TodoEditorSideEffect
import com.todo.to_do.presentation.editor.TodoEditorViewModel
import com.todo.to_do.ui.components.TaskFlowButtonSpinner
import com.todo.to_do.ui.components.TaskFlowTopLoader
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.priorityColor
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.components.toDateTimeLabel
import com.todo.to_do.ui.components.toShortLabel
import com.todo.to_do.ui.theme.TaskFlowTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditorScreen(
    todoId: String?,
    onBack: () -> Unit,
    viewModel: TodoEditorViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    var title by remember(state.todoId) { mutableStateOf(state.title) }
    var description by remember(state.todoId) { mutableStateOf(state.description.orEmpty()) }
    var dueDate by remember(state.todoId) { mutableStateOf(state.dueDate) }
    var priority by remember(state.todoId) { mutableStateOf(state.priority) }
    var reminderTime by remember(state.todoId) { mutableStateOf(state.reminderTime) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var pendingReminderDateMillis by remember { mutableStateOf<Long?>(null) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is TodoEditorSideEffect.ShowSnackbar -> toastState.show(effect.message, effect.isError)
            TodoEditorSideEffect.Saved -> onBack()
        }
    }

    LaunchedEffect(todoId) { viewModel.load(todoId) }

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
                Text(
                    text = if (state.isNew) "New task" else "Edit task",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                TextButton(onClick = onBack, enabled = !state.isSaving) { Text("Back", color = colors.accent400) }
            }
            TaskFlowTopLoader(visible = state.isLoading)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Priority", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { option ->
                    PriorityChip(
                        label = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = priority == option,
                        color = priorityColor(option),
                        onClick = { priority = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showDueDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(dueDate?.let { "Due ${it.toShortLabel()}" } ?: "Set due date")
                }
                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) { Text("Clear", color = colors.danger) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showReminderDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(reminderTime?.let { "Reminder ${it.toDateTimeLabel()}" } ?: "Set reminder")
                }
                if (reminderTime != null) {
                    TextButton(onClick = { reminderTime = null }) { Text("Clear", color = colors.danger) }
                }
            }

            if (!state.isNew) {
                OutlinedButton(
                    onClick = { viewModel.toggleComplete() },
                    enabled = !state.isSaving && !state.isCompleted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isCompleted) "Completed" else "Mark as Completed",
                        color = if (state.isCompleted) colors.textSecondary else colors.accent600
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.save(title.trim(), description.trim().ifBlank { null }, dueDate, priority, reminderTime)
                },
                enabled = !state.isSaving && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) TaskFlowButtonSpinner(modifier = Modifier.size(20.dp))
                else Text("Save")
            }
        }
        ToastHost(toastState, modifier = Modifier.padding(padding))
    }

    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = Instant.fromEpochMilliseconds(it) }
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReminderDatePicker) {
        val initialMillis = reminderTime?.toEpochMilliseconds() ?: dueDate?.toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    showReminderDatePicker = false
                    if (millis != null) {
                        pendingReminderDateMillis = millis
                        showReminderTimePicker = true
                    }
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReminderTimePicker) {
        val existingLocal = reminderTime?.toLocalDateTime(TimeZone.currentSystemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = existingLocal?.hour ?: 9,
            initialMinute = existingLocal?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pendingReminderDateMillis
                    if (millis != null) {
                        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        val combined = LocalDateTime(
                            date.year,
                            date.monthNumber,
                            date.dayOfMonth,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        reminderTime = combined.toInstant(TimeZone.currentSystemDefault())
                    }
                    showReminderTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showReminderTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TaskFlowTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else colors.surfaceMuted)
            .border(1.dp, if (selected) color else colors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) color else colors.textSecondary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
