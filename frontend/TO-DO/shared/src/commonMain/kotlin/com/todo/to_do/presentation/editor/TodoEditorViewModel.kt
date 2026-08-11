package com.todo.to_do.presentation.editor

import androidx.lifecycle.ViewModel
import com.todo.to_do.data.local.ReminderStore
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.domain.repository.TodoDraft
import com.todo.to_do.domain.usecase.CreateTodoUseCase
import com.todo.to_do.domain.usecase.GetTodoUseCase
import com.todo.to_do.domain.usecase.UpdateTodoUseCase
import com.todo.to_do.domain.usecase.ToggleCompleteUseCase
import com.todo.to_do.presentation.alarm.AlarmScheduler
import com.todo.to_do.util.toUserMessage
import com.todo.to_do.util.nowInstant
import kotlinx.datetime.Instant
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class TodoEditorViewModel(
    private val getTodo: GetTodoUseCase,
    private val createTodo: CreateTodoUseCase,
    private val updateTodo: UpdateTodoUseCase,
    private val toggleCompleteUseCase: ToggleCompleteUseCase,
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
                        reminderTime = reminderStore.get(todo.id),
                        isCompleted = todo.isCompleted
                    )
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(it.toUserMessage("Failed to load"), isError = true))
            }
    }

    fun toggleComplete() = intent {
        val todoId = state.todoId ?: return@intent
        reduce { state.copy(isSaving = true) }
        runCatching { toggleCompleteUseCase(todoId) }
            .onSuccess { updated ->
                reduce { state.copy(isCompleted = updated.isCompleted, isSaving = false) }
                if (updated.isCompleted) {
                    alarmScheduler.cancel(todoId)
                } else {
                    reminderStore.get(todoId)?.let { time ->
                        if (time > nowInstant()) alarmScheduler.schedule(todoId, updated.title, time)
                    }
                }
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(if (updated.isCompleted) "Task completed" else "Task marked pending"))
            }
            .onFailure {
                reduce { state.copy(isSaving = false) }
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(it.toUserMessage("Update failed"), isError = true))
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
                postSideEffect(TodoEditorSideEffect.ShowSnackbar(it.toUserMessage("Save failed"), isError = true))
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
