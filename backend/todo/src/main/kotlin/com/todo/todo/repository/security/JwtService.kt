package com.todo.todo.repository.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

data class JwtPrincipal(
    val userId: String,
    val email: String
)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long
) {
    private val signingKey: SecretKey by lazy {
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size < 32) {
            Keys.hmacShaKeyFor(bytes.copyOf(32))
        } else {
            Keys.hmacShaKeyFor(bytes)
        }
    }

    fun generateToken(userId: String, email: String): String {
        val now = Instant.now()
        val expiry = now.plusMillis(expirationMs)
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact()
    }

    fun parseToken(token: String): JwtPrincipal? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = claims.subject ?: return null
            val email = claims["email"] as? String ?: return null
            JwtPrincipal(userId, email)
        } catch (e: Exception) {
            null
        }
    }
}
