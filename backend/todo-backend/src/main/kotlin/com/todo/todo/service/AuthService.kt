package com.todo.todo.service

import com.todo.todo.dto.AuthResponse
import com.todo.todo.dto.LoginRequest
import com.todo.todo.dto.RegisterRequest
import com.todo.todo.exception.EmailAlreadyInUseException
import com.todo.todo.exception.InvalidCredentialsException
import com.todo.todo.model.User
import com.todo.todo.mapper.toDomain
import com.todo.todo.mapper.toEntity
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.lowercase()
        if (userRepository.existsByEmail(email)) throw EmailAlreadyInUseException(email)

        val user = User(
            id = null,
            email = email,
            passwordHash = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Password encoding failed"),
            createdAt = Instant.now()
        )
        val saved = userRepository.save(user.toEntity()).toDomain()
        return toAuthResponse(saved)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.lowercase())?.toDomain()
            ?: throw InvalidCredentialsException()
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return toAuthResponse(user)
    }

    private fun toAuthResponse(user: User): AuthResponse {
        val token = jwtService.generateToken(user.id!!, user.email)
        return AuthResponse(token = token, userId = user.id, email = user.email)
    }
}