package com.todo.todo.repository

import com.todo.todo.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.Instant

interface TodoRepository : CoroutineCrudRepository<TodoEntity, String> {

    fun findByUserIdOrderByPriorityRankAsc(userId: String): Flow<TodoEntity>

    fun findByUserIdAndCategoryOrderByPriorityRankAsc(userId: String, category: String): Flow<TodoEntity>

    @Query("{ 'userId': ?0, 'dueDate': { \$gte: ?1, \$lte: ?2 } }")
    fun findByUserIdAndDueDateBetween(userId: String, from: Instant, to: Instant): Flow<TodoEntity>

    @Query("{ 'userId': ?0, 'category': ?1, 'dueDate': { \$gte: ?2, \$lte: ?3 } }")
    fun findByUserIdAndCategoryAndDueDateBetween(
        userId: String,
        category: String,
        from: Instant,
        to: Instant
    ): Flow<TodoEntity>
}
