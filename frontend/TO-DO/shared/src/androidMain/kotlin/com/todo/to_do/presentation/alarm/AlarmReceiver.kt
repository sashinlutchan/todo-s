package com.todo.to_do.presentation.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

internal const val EXTRA_ALARM_TODO_ID = "todo_id"
internal const val EXTRA_ALARM_TITLE = "title"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            putExtra(EXTRA_ALARM_TODO_ID, intent.getStringExtra(EXTRA_ALARM_TODO_ID))
            putExtra(EXTRA_ALARM_TITLE, intent.getStringExtra(EXTRA_ALARM_TITLE))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
