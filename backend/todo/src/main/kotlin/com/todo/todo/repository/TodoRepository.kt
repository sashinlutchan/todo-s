package com.todo.todo.repository

import com.todo.todo.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

interface TodoRepository : CoroutineCrudRepository<TodoEntity, String>, CustomTodoRepository {
    fun findByUserIdOrderByPriorityRankAsc(userId: String): Flow<TodoEntity>
    suspend fun countByUserId(userId: String): Long
}

interface CustomTodoRepository {
    fun findTodos(userId: String, from: Instant?, to: Instant?, category: String?): Flow<TodoEntity>
}

@Repository
class CustomTodoRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : CustomTodoRepository {
    override fun findTodos(userId: String, from: Instant?, to: Instant?, category: String?): Flow<TodoEntity> {
        val query = Query().with(Sort.by(Sort.Order.asc("priorityRank")))
        query.addCriteria(Criteria.where("userId").`is`(userId))

        if (category != null) {
            query.addCriteria(Criteria.where("category").`is`(category))
        }

        if (from != null && to != null) {
            query.addCriteria(Criteria.where("dueDate").gte(from).lte(to))
        } else if (from != null) {
            query.addCriteria(Criteria.where("dueDate").gte(from))
        } else if (to != null) {
            query.addCriteria(Criteria.where("dueDate").lte(to))
        }

        return mongoTemplate.find(query, TodoEntity::class.java).asFlow()
    }
}
