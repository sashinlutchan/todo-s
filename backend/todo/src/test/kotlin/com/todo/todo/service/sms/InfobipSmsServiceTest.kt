package com.todo.todo.service.sms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InfobipSmsServiceTest {

    private val smsService = InfobipSmsService(
        baseUrl = "https://api.infobip.com",
        apiKey = "test-api-key",
        sender = "TaskFlow"
    )

    @Test
    fun `toMsisdn strips the leading plus sign`() {
        assertEquals("15551234567", smsService.toMsisdn("+15551234567"))
    }

    @Test
    fun `toMsisdn strips spaces, dashes, and parentheses`() {
        assertEquals("15551234567", smsService.toMsisdn("+1 (555) 123-4567"))
    }

    @Test
    fun `toMsisdn leaves an already plain-digit number unchanged`() {
        assertEquals("15551234567", smsService.toMsisdn("15551234567"))
    }
}
