package com.todo.to_do.data.local

import kotlinx.datetime.Instant

expect class ReminderStore {
    fun save(todoId: String, triggerAt: Instant)
    fun get(todoId: String): Instant?
    fun clear(todoId: String)
}

