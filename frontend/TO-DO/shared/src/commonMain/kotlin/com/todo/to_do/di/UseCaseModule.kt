package com.todo.to_do.di

import com.todo.to_do.domain.usecase.CreateTodoUseCase
import com.todo.to_do.domain.usecase.DeleteTodoUseCase
import com.todo.to_do.domain.usecase.GetTodoUseCase
import com.todo.to_do.domain.usecase.GetTodosUseCase
import com.todo.to_do.domain.usecase.LoginUseCase
import com.todo.to_do.domain.usecase.RegisterUseCase
import com.todo.to_do.domain.usecase.ReorderTodosUseCase
import com.todo.to_do.domain.usecase.ToggleCompleteUseCase
import com.todo.to_do.domain.usecase.UpdateTodoUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::GetTodosUseCase)
    factoryOf(::GetTodoUseCase)
    factoryOf(::CreateTodoUseCase)
    factoryOf(::UpdateTodoUseCase)
    factoryOf(::ToggleCompleteUseCase)
    factoryOf(::DeleteTodoUseCase)
    factoryOf(::ReorderTodosUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::LoginUseCase)
}
