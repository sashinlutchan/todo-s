package com.todo.todo.service.sms

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity

/**
 * Sends transactional SMS through the Infobip SMS API
 * (https://www.infobip.com/docs/api/channels/sms/sms-messaging/outbound-sms/send-sms-messages).
 */
@Service
class InfobipSmsService(
    @Value("\${app.infobip.base-url}") private val baseUrl: String,
    @Value("\${app.infobip.api-key}") private val apiKey: String,
    @Value("\${app.infobip.sms-sender}") private val sender: String
) : SmsService {

    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    override suspend fun sendResetCode(to: String, displayName: String, code: String) {
        send(to, "Hi $displayName, your TaskFlow password reset code is $code. It expires in 15 minutes.")
    }

    override suspend fun sendVerificationCode(to: String, displayName: String, code: String) {
        send(to, "Hi $displayName, your TaskFlow verification code is $code. It expires in 15 minutes.")
    }

    private suspend fun send(to: String, text: String) {
        webClient.post()
            .uri("/sms/3/messages")
            .header(HttpHeaders.AUTHORIZATION, "App $apiKey")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                SmsRequest(
                    messages = listOf(
                        SmsMessage(
                            destinations = listOf(SmsDestination(to = toMsisdn(to))),
                            sender = sender,
                            content = SmsContent(text = text)
                        )
                    )
                )
            )
            .retrieve()
            .awaitBodilessEntity()
    }

    // Infobip expects the destination as a plain-digit MSISDN with country code and no leading
    // '+' — sending the '+' (or any non-digit) causes a "Mobile operator not found" / 345 rejection.
    internal fun toMsisdn(phoneNumber: String): String = phoneNumber.filter { it.isDigit() }
}

private data class SmsRequest(val messages: List<SmsMessage>)
private data class SmsMessage(val destinations: List<SmsDestination>, val sender: String, val content: SmsContent)
private data class SmsDestination(val to: String)
private data class SmsContent(val text: String)
