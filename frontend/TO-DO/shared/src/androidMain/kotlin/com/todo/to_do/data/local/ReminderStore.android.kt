package com.todo.to_do.data.local

import android.content.Context
import kotlinx.datetime.Instant

actual class ReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("task_flow_reminders", Context.MODE_PRIVATE)

    actual fun save(todoId: String, triggerAt: Instant) {
        prefs.edit().putLong(todoId, triggerAt.toEpochMilliseconds()).apply()
    }

    actual fun get(todoId: String): Instant? {
        if (!prefs.contains(todoId)) return null
        return Instant.fromEpochMilliseconds(prefs.getLong(todoId, 0L))
    }

    actual fun clear(todoId: String) {
        prefs.edit().remove(todoId).apply()
    }
}

