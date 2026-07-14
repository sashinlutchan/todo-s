package com.todo.todo.service

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.AuthResponse
import com.todo.todo.entity.UserEntity
import com.todo.todo.exception.EmailAlreadyInUseException
import com.todo.todo.exception.InvalidCredentialsException
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    suspend fun register(request: AuthRequest): AuthResponse {
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser != null) {
            throw EmailAlreadyInUseException(request.email)
        }

        val hashedPassword = passwordEncoder.encode(request.password)!!
        val userEntity = UserEntity(
            email = request.email,
            passwordHash = hashedPassword
        )

        val savedUser = userRepository.save(userEntity)
        val userId = savedUser.id!!
        val token = jwtService.generateToken(userId, savedUser.email)

        return AuthResponse(
            token = token,
            userId = userId,
            email = savedUser.email
        )
    }

    suspend fun login(request: AuthRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val userId = user.id!!
        val token = jwtService.generateToken(userId, user.email)

        return AuthResponse(
            token = token,
            userId = userId,
            email = user.email
        )
    }
}
