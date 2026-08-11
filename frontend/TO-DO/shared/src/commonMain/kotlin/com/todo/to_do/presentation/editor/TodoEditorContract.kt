package com.todo.to_do.presentation.editor

import com.todo.to_do.domain.model.Priority
import kotlinx.datetime.Instant

data class TodoEditorState(
    val todoId: String? = null,
    val title: String = "",
    val description: String? = null,
    val dueDate: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val priorityRank: Int = Int.MAX_VALUE,
    val category: String? = null,
    val reminderTime: Instant? = null,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
) {
    val isNew: Boolean get() = todoId == null
}

sealed interface TodoEditorSideEffect {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : TodoEditorSideEffect
    data object Saved : TodoEditorSideEffect
}
