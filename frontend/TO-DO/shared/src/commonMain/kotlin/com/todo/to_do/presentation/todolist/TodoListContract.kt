package com.todo.to_do.presentation.todolist

import com.todo.to_do.domain.model.Todo
import com.todo.to_do.util.nowInstant

enum class TodoTab { ACTIVE, OVERDUE, COMPLETED }

data class TodoListState(
    val todos: List<Todo> = emptyList(),
    val selectedTab: TodoTab = TodoTab.ACTIVE,
    val isLoading: Boolean = false
) {
    val visibleTodos: List<Todo> get() = todos.filterFor(selectedTab)
}

fun List<Todo>.filterFor(tab: TodoTab): List<Todo> {
    val now = nowInstant()
    return when (tab) {
        TodoTab.ACTIVE -> filter { !it.isCompleted && (it.dueDate == null || it.dueDate >= now) }
        TodoTab.OVERDUE -> filter { !it.isCompleted && it.dueDate != null && it.dueDate < now }
        TodoTab.COMPLETED -> filter { it.isCompleted }
    }
}

sealed interface TodoListSideEffect {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : TodoListSideEffect
}
