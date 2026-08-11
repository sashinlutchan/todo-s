package com.todo.to_do.di

import com.todo.to_do.presentation.auth.AuthViewModel
import com.todo.to_do.presentation.editor.TodoEditorViewModel
import com.todo.to_do.presentation.forgotpassword.ForgotPasswordViewModel
import com.todo.to_do.presentation.profile.ProfileViewModel
import com.todo.to_do.presentation.resetcode.VerifyResetCodeViewModel
import com.todo.to_do.presentation.resetpassword.ResetPasswordViewModel
import com.todo.to_do.presentation.todolist.TodoListViewModel
import com.todo.to_do.presentation.verifyemail.VerifyEmailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::TodoListViewModel)
    viewModelOf(::TodoEditorViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::VerifyResetCodeViewModel)
    viewModelOf(::ResetPasswordViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::VerifyEmailViewModel)
}
