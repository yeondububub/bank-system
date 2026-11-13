package com.bank.system.api.config

import com.bank.system.domain.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.expiration-ms:86400000}")
    private val expirationMs: Long = 86400000L
) {
    private val objectMapper = ObjectMapper()

    init {
        require(secretKey.isNotBlank()) { "JWT secretKey는 비어 있을 수 없습니다." }
    }

    data class TokenClaims(
        val userId: Long,
        val email: String,
        val role: UserRole,
        val issuedAtMs: Long,
        val expiresAtMs: Long
    )

    fun createToken(userId: Long, email: String, role: UserRole = UserRole.USER): String {
        val nowMs = System.currentTimeMillis()
        val expiryMs = nowMs + expirationMs

        val payloadMap = linkedMapOf(
            "sub" to userId.toString(),
            "email" to email,
            "role" to role.name,
            "iat" to (nowMs / 1000),
            "exp" to (expiryMs / 1000),
            "expMs" to expiryMs
        )

        val encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payloadMap))
        val signingInput = "$ENCODED_HEADER.$encodedPayload"
        val signature = hmacSha256(signingInput, secretKey)

        return "$signingInput.$signature"
    }

    fun validateToken(token: String): Boolean {
        return parseClaimsOrNull(token) != null
    }

    fun parseClaims(token: String): TokenClaims {
        return parseClaimsOrNull(token)
            ?: throw IllegalArgumentException("유효하지 않거나 만료된 JWT 토큰입니다.")
    }

    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).userId
    }

    fun getEmailFromToken(token: String): String {
        return parseClaims(token).email
    }

    fun getUserRoleFromToken(token: String): UserRole {
        return try {
            parseClaims(token).role
        } catch (e: Exception) {
            UserRole.USER
        }
    }

    private fun parseClaimsOrNull(token: String): TokenClaims? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val encodedHeader = parts[0]
            val encodedPayload = parts[1]
            val providedSignature = parts[2]

            if (encodedHeader != ENCODED_HEADER) return null

            // 1. HMAC-SHA256 서명 상수 시간(Constant-time) 검증으로 타이밍 공격 방어
            val signingInput = "$encodedHeader.$encodedPayload"
            val expectedSignature = hmacSha256(signingInput, secretKey)
            val isSignatureValid = MessageDigest.isEqual(
                providedSignature.toByteArray(StandardCharsets.UTF_8),
                expectedSignature.toByteArray(StandardCharsets.UTF_8)
            )
            if (!isSignatureValid) return null

            // 2. JSON 클레임 파싱 및 만료 시각 검증
            val payloadBytes = Base64.getUrlDecoder().decode(encodedPayload)
            val node = objectMapper.readTree(payloadBytes)

            val sub = node.get("sub")?.asText()?.toLongOrNull() ?: return null
            val email = node.get("email")?.asText()?.takeIf { it.isNotBlank() } ?: return null
            val roleStr = node.get("role")?.asText() ?: return null
            val role = UserRole.entries.find { it.name == roleStr } ?: return null
            val iatSeconds = node.get("iat")?.asLong() ?: 0L
            val expiresAtMs = node.get("expMs")?.asLong()
                ?: node.get("exp")?.asLong()?.times(1000L)
                ?: return null

            if (System.currentTimeMillis() >= expiresAtMs) return null

            TokenClaims(
                userId = sub,
                email = email,
                role = role,
                issuedAtMs = iatSeconds * 1000L,
                expiresAtMs = expiresAtMs
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun hmacSha256(data: String, key: String): String {
        val sha256Hmac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKeySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
        sha256Hmac.init(secretKeySpec)
        val hash = sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return base64UrlEncode(hash)
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private val ENCODED_HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
    }
}
