package com.todo.to_do.di

import com.todo.to_do.data.local.ReminderStore
import com.todo.to_do.data.local.SessionPersistence
import com.todo.to_do.presentation.alarm.AlarmScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { SessionPersistence() }
    single { ReminderStore() }
    single { AlarmScheduler() }
}
