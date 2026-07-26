package com.bank.system.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.expiration-ms:86400000}")
    private val expirationMs: Long = 86400000L
) {

    fun createToken(userId: Long, email: String): String {
        val expiry = System.currentTimeMillis() + expirationMs
        val payload = "$userId:$email:$expiry"
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val signature = hmacSha256(encodedPayload, secretKey)

        return "$encodedPayload.$signature"
    }

    fun validateToken(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 2) return false

            val encodedPayload = parts[0]
            val signature = parts[1]

            // 1. HMAC-SHA256 위변조 검증
            val expectedSignature = hmacSha256(encodedPayload, secretKey)
            if (signature != expectedSignature) return false

            // 2. 만료 시간 검증
            val decodedPayload = String(Base64.getUrlDecoder().decode(encodedPayload))
            val expiry = decodedPayload.split(":")[2].toLongOrNull() ?: return false

            System.currentTimeMillis() < expiry
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        val encodedPayload = token.split(".")[0]
        val decodedPayload = String(Base64.getUrlDecoder().decode(encodedPayload))
        return decodedPayload.split(":")[0].toLong()
    }

    private fun hmacSha256(data: String, key: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKeySpec)
        val hash = sha256Hmac.doFinal(data.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
}
