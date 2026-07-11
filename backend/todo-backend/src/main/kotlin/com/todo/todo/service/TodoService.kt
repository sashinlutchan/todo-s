package com.todo.todo.service

import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEntity
import com.todo.todo.dto.SyncEvent
import com.todo.todo.dto.TodoRequest
import com.todo.todo.exception.ForbiddenResourceException
import com.todo.todo.exception.TodoNotFoundException
import com.todo.todo.model.Todo
import com.todo.todo.mapper.toDomain
import com.todo.todo.mapper.toEntity
import com.todo.todo.repository.TodoRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TodoService(
    private val todoRepository: TodoRepository,
    private val syncBroadcastService: SyncBroadcastService
) {

    suspend fun listTodos(userId: String, from: Instant?, to: Instant?, category: String?): List<Todo> {
        val entities = when {
            from != null && to != null && category != null ->
                todoRepository.findByUserIdAndCategoryAndDueDateBetween(userId, category, from, to)
            from != null && to != null ->
                todoRepository.findByUserIdAndDueDateBetween(userId, from, to)
            category != null ->
                todoRepository.findByUserIdAndCategoryOrderByPriorityRankAsc(userId, category)
            else ->
                todoRepository.findByUserIdOrderByPriorityRankAsc(userId)
        }
        return entities.map { it.toDomain() }.toList()
    }

    suspend fun getTodo(userId: String, id: String): Todo {
        val entity = todoRepository.findById(id) ?: throw TodoNotFoundException(id)
        if (entity.userId != userId) throw ForbiddenResourceException()
        return entity.toDomain()
    }

    suspend fun createTodo(userId: String, request: TodoRequest): Todo {
        val now = Instant.now()
        val todo = Todo(
            id = null,
            userId = userId,
            title = request.title,
            description = request.description,
            dueDate = request.dueDate,
            priority = request.priority,
            priorityRank = request.priorityRank,
            category = request.category,
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        val saved = todoRepository.save(todo.toEntity()).toDomain()
        emit(saved.userId, SyncAction.CREATED, saved.id!!)
        return saved
    }

    suspend fun updateTodo(userId: String, id: String, request: TodoRequest): Todo {
        val existing = getTodo(userId, id)
        val updated = existing.copy(
            title = request.title,
            description = request.description,
            dueDate = request.dueDate,
            priority = request.priority,
            priorityRank = request.priorityRank,
            category = request.category,
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(updated.toEntity()).toDomain()
        emit(saved.userId, SyncAction.UPDATED, saved.id!!)
        return saved
    }

    suspend fun toggleComplete(userId: String, id: String): Todo {
        val existing = getTodo(userId, id)
        val updated = existing.copy(
            isCompleted = !existing.isCompleted,
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(updated.toEntity()).toDomain()
        emit(saved.userId, SyncAction.UPDATED, saved.id!!)
        return saved
    }

    suspend fun deleteTodo(userId: String, id: String) {
        val existing = getTodo(userId, id)
        todoRepository.deleteById(existing.id!!)
        emit(userId, SyncAction.DELETED, existing.id)
    }

    suspend fun reorder(userId: String, orderedIds: List<String>): List<Todo> {
        val result = mutableListOf<Todo>()
        orderedIds.forEachIndexed { index, id ->
            val existing = getTodo(userId, id)
            val updated = existing.copy(priorityRank = index, updatedAt = Instant.now())
            result += todoRepository.save(updated.toEntity()).toDomain()
        }
        result.forEach { emit(userId, SyncAction.UPDATED, it.id!!) }
        return result
    }

    private fun emit(userId: String, action: SyncAction, entityId: String) {
        syncBroadcastService.broadcast(SyncEvent(userId, SyncEntity.TODO, action, entityId))
    }
}
