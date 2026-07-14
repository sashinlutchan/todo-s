package com.todo.todo.controller

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.AuthResponse
import com.todo.todo.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@RequestBody request: AuthRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AuthRequest): AuthResponse {
        return authService.login(request)
    }
}
