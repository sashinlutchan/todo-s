package com.todo.to_do.data.local

import kotlinx.datetime.Instant
import platform.Foundation.NSUserDefaults

actual class ReminderStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private fun key(todoId: String) = "task_flow_reminder_$todoId"

    actual fun save(todoId: String, triggerAt: Instant) {
        defaults.setDouble(triggerAt.toEpochMilliseconds().toDouble(), key(todoId))
    }

    actual fun get(todoId: String): Instant? {
        val key = key(todoId)
        if (defaults.objectForKey(key) == null) return null
        return Instant.fromEpochMilliseconds(defaults.doubleForKey(key).toLong())
    }

    actual fun clear(todoId: String) {
        defaults.removeObjectForKey(key(todoId))
    }
}
