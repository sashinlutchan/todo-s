package com.todo.todo.controller

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.AuthResponse
import com.todo.todo.dto.ForgotPasswordRequest
import com.todo.todo.dto.ForgotPasswordResponse
import com.todo.todo.dto.RegisterResponse
import com.todo.todo.dto.ResendVerificationCodeRequest
import com.todo.todo.dto.ResendVerificationCodeResponse
import com.todo.todo.dto.ResetPasswordRequest
import com.todo.todo.dto.ResetPasswordResponse
import com.todo.todo.dto.UserProfileDto
import com.todo.todo.dto.VerifyEmailRequest
import com.todo.todo.dto.VerifyEmailResponse
import com.todo.todo.dto.VerifyResetCodeRequest
import com.todo.todo.dto.VerifyResetCodeResponse
import com.todo.todo.dto.VerifyTokenResponse
import com.todo.todo.repository.security.currentUserId
import com.todo.todo.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    companion object {
        private val log = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@RequestBody request: AuthRequest): RegisterResponse {
        log.info("Register attempt email={}", request.email)
        return authService.register(request).also {
            log.info("Register success email={}", request.email)
        }
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AuthRequest): AuthResponse {
        log.info("Login attempt email={}", request.email)
        return authService.login(request).also {
            log.info("Login success email={} userId={}", request.email, it.userId)
        }
    }

    @GetMapping("/verify")
    suspend fun verifyToken(
        @RequestHeader(value = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<VerifyTokenResponse> {
        val token = authHeader?.removePrefix("Bearer ")?.trim()
        if (token.isNullOrEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(VerifyTokenResponse(valid = false, reason = "TOKEN_INVALID"))
        }

        val response = authService.verifyToken(token)
        val status = if (response.valid) HttpStatus.OK else HttpStatus.UNAUTHORIZED
        return ResponseEntity.status(status).body(response)
    }

    @GetMapping("/profile")
    suspend fun getProfile(): UserProfileDto {
        val userId = currentUserId()
        return authService.getProfile(userId)
    }

    @PostMapping("/forgot-password")
    suspend fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ForgotPasswordResponse {
        log.info("Forgot-password request email={}", request.email)
        return authService.sendForgotPasswordCode(request)
    }

    @PostMapping("/verify-reset-code")
    suspend fun verifyResetCode(
        @RequestBody request: VerifyResetCodeRequest
    ): ResponseEntity<VerifyResetCodeResponse> {
        val response = authService.verifyResetCode(request)
        val status = if (response.valid) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(response)
    }

    @PostMapping("/reset-password")
    suspend fun resetPassword(
        @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<ResetPasswordResponse> {
        val response = authService.resetPassword(request)
        val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(response)
    }

    @PostMapping("/verify-email")
    suspend fun verifyEmail(
        @RequestBody request: VerifyEmailRequest
    ): ResponseEntity<VerifyEmailResponse> {
        val response = authService.verifyEmail(request)
        val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(response)
    }

    @PostMapping("/resend-verification-code")
    suspend fun resendVerificationCode(
        @RequestBody request: ResendVerificationCodeRequest
    ): ResendVerificationCodeResponse {
        return authService.resendVerificationCode(request)
    }
}
