package com.todo.to_do.di

import com.todo.to_do.data.remote.ApiConfig
import com.todo.to_do.data.remote.SessionStore
import com.todo.to_do.data.remote.SyncClient
import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.createHttpClient
import org.koin.dsl.module

val networkModule = module {
    single {
        ApiConfig(
            baseUrl = getProperty("BASE_URL"),
            wsUrl = getProperty("WS_URL")
        )
    }
    single { SessionStore(get()) }
    single { createHttpClient(get(), get()) }
    single { TaskFlowApi(get()) }
    single { SyncClient(get(), get(), get()) }
}

