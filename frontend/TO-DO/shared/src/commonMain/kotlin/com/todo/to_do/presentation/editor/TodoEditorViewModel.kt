package com.todo.to_do.presentation.editor

import androidx.lifecycle.ViewModel
import com.todo.to_do.data.local.ReminderStore
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.domain.repository.TodoDraft
import com.todo.to_do.domain.usecase.CreateTodoUseCase
import com.todo.to_do.domain.usecase.GetTodoUseCase
import com.todo.to_do.domain.usecase.UpdateTodoUseCase
import com.todo.to_do.presentation.alarm.AlarmScheduler
import kotlinx.datetime.Instant
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TodoEditorViewModel(
    private val getTodo: GetTodoUseCase,
    private val createTodo: CreateTodoUseCase,
    private val updateTodo: UpdateTodoUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val reminderStore: ReminderStore
) : ViewModel(), ContainerHost<TodoEditorState, TodoEditorSideEffect> {

    override val container = container<TodoEditorState, TodoEditorSideEffect>(TodoEditorState())

    fun load(id: String?) = intent {
        if (id == null) {
            reduce { TodoEditorState() }
            return@intent
        }
        reduce { state.copy(isLoading = true) }
        runCatching { getTodo(id) }
            .onSuccess { todo ->
                reduce {
                    TodoEditorState(
                        todoId = todo.id,
                        title = todo.title,
                        description = todo.description,
                        dueDate = todo.dueDate,
                        priority = todo.priority,
                        priorityRank = todo.priorityRank,
                        category = todo.category,
                        reminderTime = reminderStore.get(todo.id)
                    )
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(it.message ?: "Failed to load", isError = true))
            }
    }

    fun save(title: String, description: String?, dueDate: Instant?, priority: Priority, reminderTime: Instant?) =
        intent {
            if (title.isBlank()) {
                postSideEffect(TodoEditorSideEffect.ShowSnackbar("Title is required", isError = true))
                return@intent
            }
            reduce { state.copy(isSaving = true) }
            val draft = TodoDraft(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                priorityRank = state.priorityRank,
                category = state.category
            )
            runCatching {
                if (state.todoId == null) createTodo(draft) else updateTodo(state.todoId!!, draft)
            }.onSuccess { saved ->
                reconcileReminder(saved.id, saved.title, reminderTime)
                reduce { state.copy(todoId = saved.id, isSaving = false) }
                postSideEffect(TodoEditorSideEffect.Saved)
            }.onFailure {
                reduce { state.copy(isSaving = false) }
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(it.message ?: "Save failed", isError = true))
            }
        }

    private fun reconcileReminder(todoId: String, title: String, reminderTime: Instant?) {
        if (reminderTime == null) {
            alarmScheduler.cancel(todoId)
            reminderStore.clear(todoId)
        } else {
            alarmScheduler.schedule(todoId, title, reminderTime)
            reminderStore.save(todoId, reminderTime)
        }
    }
}
