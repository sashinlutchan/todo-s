package com.todo.todo.repository.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
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

sealed interface TokenValidation {
    data class Valid(val principal: JwtPrincipal, val expiresAt: Instant) : TokenValidation
    data object Expired : TokenValidation
    data object Invalid : TokenValidation
}

sealed interface ResetTokenValidation {
    data class Valid(val email: String) : ResetTokenValidation
    data object Expired : ResetTokenValidation
    data object Invalid : ResetTokenValidation
}

private const val RESET_TOKEN_PURPOSE = "password_reset"

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long,
    @Value("\${app.jwt.reset-token-expiration-ms:300000}") private val resetTokenExpirationMs: Long = 300_000L
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

    /** Like [parseToken], but distinguishes an expired signature from a malformed/forged one. */
    fun validateToken(token: String): TokenValidation {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = claims.subject ?: return TokenValidation.Invalid
            val email = claims["email"] as? String ?: return TokenValidation.Invalid
            TokenValidation.Valid(JwtPrincipal(userId, email), claims.expiration.toInstant())
        } catch (e: ExpiredJwtException) {
            TokenValidation.Expired
        } catch (e: JwtException) {
            TokenValidation.Invalid
        } catch (e: IllegalArgumentException) {
            TokenValidation.Invalid
        }
    }

    /** Short-lived token proving the holder just verified a password-reset OTP for [email]. */
    fun generateResetToken(email: String): String {
        val now = Instant.now()
        val expiry = now.plusMillis(resetTokenExpirationMs)
        return Jwts.builder()
            .subject(email)
            .claim("purpose", RESET_TOKEN_PURPOSE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact()
    }

    fun validateResetToken(token: String): ResetTokenValidation {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val email = claims.subject ?: return ResetTokenValidation.Invalid
            val purpose = claims["purpose"] as? String
            if (purpose != RESET_TOKEN_PURPOSE) return ResetTokenValidation.Invalid
            ResetTokenValidation.Valid(email)
        } catch (e: ExpiredJwtException) {
            ResetTokenValidation.Expired
        } catch (e: JwtException) {
            ResetTokenValidation.Invalid
        } catch (e: IllegalArgumentException) {
            ResetTokenValidation.Invalid
        }
    }
}
