package com.todo.to_do.di

import com.todo.to_do.domain.usecase.CreateTodoUseCase
import com.todo.to_do.domain.usecase.DeleteTodoUseCase
import com.todo.to_do.domain.usecase.ForgotPasswordUseCase
import com.todo.to_do.domain.usecase.GetProfileUseCase
import com.todo.to_do.domain.usecase.GetTodoUseCase
import com.todo.to_do.domain.usecase.GetTodosUseCase
import com.todo.to_do.domain.usecase.LoginUseCase
import com.todo.to_do.domain.usecase.LogoutUseCase
import com.todo.to_do.domain.usecase.RegisterUseCase
import com.todo.to_do.domain.usecase.ReorderTodosUseCase
import com.todo.to_do.domain.usecase.ResendVerificationCodeUseCase
import com.todo.to_do.domain.usecase.ResetPasswordUseCase
import com.todo.to_do.domain.usecase.ToggleCompleteUseCase
import com.todo.to_do.domain.usecase.UpdateTodoUseCase
import com.todo.to_do.domain.usecase.VerifyEmailUseCase
import com.todo.to_do.domain.usecase.VerifyResetCodeUseCase
import com.todo.to_do.domain.usecase.VerifyTokenUseCase
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
    factoryOf(::VerifyTokenUseCase)
    factoryOf(::GetProfileUseCase)
    factoryOf(::ForgotPasswordUseCase)
    factoryOf(::VerifyResetCodeUseCase)
    factoryOf(::ResetPasswordUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::VerifyEmailUseCase)
    factoryOf(::ResendVerificationCodeUseCase)
}
