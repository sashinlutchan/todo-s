package com.todo.todo.service

import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEvent
import com.todo.todo.dto.TodoRequest
import com.todo.todo.entity.TodoEntity
import com.todo.todo.exception.ForbiddenResourceException
import com.todo.todo.exception.TodoNotFoundException
import com.todo.todo.mapper.toDomain
import com.todo.todo.mapper.toEntity
import com.todo.todo.model.Todo
import com.todo.todo.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import java.time.Instant

@Service
class TodoService(
    private val todoRepository: TodoRepository,
    private val syncBroadcastService: SyncBroadcastService
) {
    companion object {
        private val log = LoggerFactory.getLogger(TodoService::class.java)
    }

    fun getTodos(userId: String, from: Instant?, to: Instant?, category: String?): Flow<Todo> {
        log.info("Fetching todos for user={} from={} to={} category={}", userId, from, to, category)
        return todoRepository.findTodos(userId, from, to, category)
            .map { it.toDomain() }
    }

    suspend fun getTodo(id: String, userId: String): Todo {
        val entity = todoRepository.findById(id) ?: throw TodoNotFoundException(id)
        if (entity.userId != userId) {
            log.warn("Forbidden access to todo id={} by user={}", id, userId)
            throw ForbiddenResourceException()
        }
        return entity.toDomain()
    }

    suspend fun createTodo(request: TodoRequest, userId: String): Todo {
        log.info("Creating todo for user={} title={}", userId, request.title)
        val count = todoRepository.countByUserId(userId).toInt()
        val entity = TodoEntity(
            userId = userId,
            title = request.title,
            description = request.description,
            isCompleted = request.isCompleted,
            priority = request.priority.name,
            category = request.category,
            dueDate = request.dueDate,
            priorityRank = count,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(entity).toDomain()
        syncBroadcastService.broadcast(
            SyncEvent(
                userId = userId,
                entity = saved,
                action = SyncAction.CREATED,
                entityId = saved.id!!
            )
        )
        return saved
    }

    suspend fun updateTodo(id: String, request: TodoRequest, userId: String): Todo {
        log.info("Updating todo id={} for user={}", id, userId)
        val existing = todoRepository.findById(id) ?: throw TodoNotFoundException(id)
        if (existing.userId != userId) {
            log.warn("Forbidden update of todo id={} by user={}", id, userId)
            throw ForbiddenResourceException()
        }
        val updatedEntity = existing.copy(
            title = request.title,
            description = request.description,
            isCompleted = request.isCompleted,
            priority = request.priority.name,
            category = request.category,
            dueDate = request.dueDate,
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(updatedEntity).toDomain()
        syncBroadcastService.broadcast(
            SyncEvent(
                userId = userId,
                entity = saved,
                action = SyncAction.UPDATED,
                entityId = saved.id!!
            )
        )
        return saved
    }

    suspend fun completeTodo(id: String, userId: String): Todo {
        log.info("Toggling complete for todo id={} user={}", id, userId)
        val existing = todoRepository.findById(id) ?: throw TodoNotFoundException(id)
        if (existing.userId != userId) {
            log.warn("Forbidden complete of todo id={} by user={}", id, userId)
            throw ForbiddenResourceException()
        }
        val updatedEntity = existing.copy(
            isCompleted = true,
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(updatedEntity).toDomain()
        syncBroadcastService.broadcast(
            SyncEvent(
                userId = userId,
                entity = saved,
                action = SyncAction.UPDATED,
                entityId = saved.id!!
            )
        )
        return saved
    }

    suspend fun deleteTodo(id: String, userId: String) {
        log.info("Deleting todo id={} for user={}", id, userId)
        val existing = todoRepository.findById(id) ?: throw TodoNotFoundException(id)
        if (existing.userId != userId) {
            log.warn("Forbidden delete of todo id={} by user={}", id, userId)
            throw ForbiddenResourceException()
        }
        todoRepository.delete(existing)
        syncBroadcastService.broadcast(
            SyncEvent(
                userId = userId,
                entity = null,
                action = SyncAction.DELETED,
                entityId = id
            )
        )
    }

    suspend fun reorderTodos(orderedIds: List<String>, userId: String) {
        log.info("Reordering {} todos for user={}", orderedIds.size, userId)
        val userTodos = todoRepository.findByUserIdOrderByPriorityRankAsc(userId).toList().associateBy { it.id }
        val updatedEntities = mutableListOf<TodoEntity>()
        orderedIds.forEachIndexed { index, id ->
            val existing = userTodos[id]
            if (existing != null && existing.priorityRank != index) {
                updatedEntities.add(existing.copy(priorityRank = index, updatedAt = Instant.now()))
            }
        }
        if (updatedEntities.isNotEmpty()) {
            todoRepository.saveAll(updatedEntities).collect { savedEntity ->
                val domain = savedEntity.toDomain()
                syncBroadcastService.broadcast(
                    SyncEvent(
                        userId = userId,
                        entity = domain,
                        action = SyncAction.UPDATED,
                        entityId = domain.id!!
                    )
                )
            }
        }
    }
}
