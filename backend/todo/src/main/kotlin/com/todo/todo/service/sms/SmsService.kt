package com.todo.todo.service.sms

interface SmsService {
    suspend fun sendResetCode(to: String, displayName: String, code: String)
    suspend fun sendVerificationCode(to: String, displayName: String, code: String)
}

