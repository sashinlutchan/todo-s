package com.todo.to_do.presentation.todolist

import androidx.lifecycle.ViewModel
import com.todo.to_do.data.local.ReminderStore
import com.todo.to_do.domain.usecase.DeleteTodoUseCase
import com.todo.to_do.domain.usecase.GetTodosUseCase
import com.todo.to_do.domain.usecase.ReorderTodosUseCase
import com.todo.to_do.domain.usecase.ToggleCompleteUseCase
import com.todo.to_do.presentation.alarm.AlarmScheduler
import com.todo.to_do.util.nowInstant
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TodoListViewModel(
    private val getTodos: GetTodosUseCase,
    private val toggleComplete: ToggleCompleteUseCase,
    private val deleteTodo: DeleteTodoUseCase,
    private val reorderTodos: ReorderTodosUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val reminderStore: ReminderStore
) : ViewModel(), ContainerHost<TodoListState, TodoListSideEffect> {

    override val container = container<TodoListState, TodoListSideEffect>(TodoListState())

    fun loadTodos() = intent {
        reduce { state.copy(isLoading = true) }
        runCatching { getTodos() }
            .onSuccess { todos -> reduce { state.copy(todos = todos, isLoading = false) } }
            .onFailure { failure ->
                reduce { state.copy(isLoading = false) }
                postSideEffect(TodoListSideEffect.ShowSnackbar(failure.toUserMessage("Failed to load"), isError = true))
            }
    }

    fun selectTab(tab: TodoTab) = intent {
        reduce { state.copy(selectedTab = tab) }
    }

    fun toggle(id: String) = intent {
        runCatching { toggleComplete(id) }
            .onSuccess { updated ->
                reduce { state.copy(todos = state.todos.map { if (it.id == id) updated else it }) }
                if (updated.isCompleted) {
                    alarmScheduler.cancel(id)
                } else {
                    reminderStore.get(id)?.let { time ->
                        if (time > nowInstant()) alarmScheduler.schedule(id, updated.title, time)
                    }
                }
                postSideEffect(TodoListSideEffect.ShowSnackbar("Updated"))
            }
            .onFailure { postSideEffect(TodoListSideEffect.ShowSnackbar(it.toUserMessage("Update failed"), isError = true)) }
    }

    fun delete(id: String) = intent {
        runCatching { deleteTodo(id) }
            .onSuccess {
                reduce { state.copy(todos = state.todos.filterNot { it.id == id }) }
                alarmScheduler.cancel(id)
                reminderStore.clear(id)
                postSideEffect(TodoListSideEffect.ShowSnackbar("Deleted", isError = true))
            }
            .onFailure { postSideEffect(TodoListSideEffect.ShowSnackbar(it.toUserMessage("Delete failed"), isError = true)) }
    }

    fun reorder(orderedIds: List<String>) = intent {
        val optimistic = orderedIds.mapNotNull { id -> state.todos.firstOrNull { it.id == id } }
        val rest = state.todos.filterNot { todo -> orderedIds.contains(todo.id) }
        reduce { state.copy(todos = optimistic + rest) }
        runCatching { reorderTodos(orderedIds) }
            .onSuccess { reordered -> reduce { state.copy(todos = reordered + rest) } }
            .onFailure { postSideEffect(TodoListSideEffect.ShowSnackbar(it.toUserMessage("Reorder failed"), isError = true)) }
    }
}
