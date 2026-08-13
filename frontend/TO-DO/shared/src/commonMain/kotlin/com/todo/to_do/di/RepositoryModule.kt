package com.todo.to_do.di

import com.todo.to_do.data.repository.AuthRepositoryImpl
import com.todo.to_do.data.repository.TodoRepositoryImpl
import com.todo.to_do.domain.repository.AuthRepository
import com.todo.to_do.domain.repository.TodoRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::TodoRepositoryImpl) { bind<TodoRepository>() }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
}

