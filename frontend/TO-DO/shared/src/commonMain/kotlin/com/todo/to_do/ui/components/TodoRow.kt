package com.todo.to_do.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.domain.model.Todo
import com.todo.to_do.ui.theme.TaskFlowTheme

@Composable
fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = TaskFlowTheme.colors
    val rowAlpha by animateFloatAsState(if (todo.isCompleted) 0.6f else 1f)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceCard)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .alpha(rowAlpha)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dragHandle()
        PriorityDot(todo.priority)
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { if (!todo.isCompleted) onToggle() },
            enabled = !todo.isCompleted,
            colors = CheckboxDefaults.colors(checkedColor = colors.accent600)
        )
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = todo.title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
            )
            todo.dueDate?.let {
                Text(
                    text = it.toShortLabel(),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (todo.isCompleted) {
                CompletedBadge()
            }
        }
        IconButton(onClick = { showDeleteConfirm = true }) {
            Text(text = "🗑", color = colors.danger)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this task?") },
            text = { Text(todo.title, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CompletedBadge() {
    val colors = TaskFlowTheme.colors
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.accent50)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "✓", color = colors.accent600, style = MaterialTheme.typography.labelSmall)
        Text(text = "Done", color = colors.accent600, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun priorityColor(priority: Priority): Color {
    val colors = TaskFlowTheme.colors
    return when (priority) {
        Priority.HIGH -> colors.danger
        Priority.MEDIUM -> colors.warning
        Priority.LOW -> colors.accent400
    }
}

@Composable
fun PriorityDot(priority: Priority) {
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(priorityColor(priority)))
}

