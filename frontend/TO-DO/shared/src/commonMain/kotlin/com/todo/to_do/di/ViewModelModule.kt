package com.todo.to_do.di

import com.todo.to_do.presentation.auth.AuthViewModel
import com.todo.to_do.presentation.editor.TodoEditorViewModel
import com.todo.to_do.presentation.todolist.TodoListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::TodoListViewModel)
    viewModelOf(::TodoEditorViewModel)
    viewModelOf(::AuthViewModel)
}
