package com.bank.system.api.exception

import com.bank.system.common.dto.ErrorResponse
import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        val errorCode = ex.errorCode
        log.warn("BusinessException: ${errorCode.code} - ${ex.message}")
        val response = ErrorResponse(
            code = errorCode.code,
            message = ex.message ?: errorCode.message
        )
        return ResponseEntity.status(errorCode.status).body(response)
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleCallNotPermittedException(ex: CallNotPermittedException): ResponseEntity<ErrorResponse> {
        log.error("CircuitBreaker is OPEN: ${ex.message}")
        val response = ErrorResponse(
            code = ErrorCode.EXTERNAL_SERVER_ERROR.code,
            message = "현재 접속량이 많아 결제 시스템 연동이 지연되고 있습니다. 잠시 후 다시 시도해주세요."
        )
        return ResponseEntity.status(ErrorCode.EXTERNAL_SERVER_ERROR.status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "잘못된 요청입니다."
        val response = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = errorMessage
        )
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(response)
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleStandardExceptions(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        log.warn("StandardException: ${ex.message}")
        val response = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = ex.message ?: "잘못된 요청 상태입니다."
        )
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("UnhandledException: ${ex.message}", ex)
        val response = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message
        )
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status).body(response)
    }

    @ExceptionHandler(com.bank.system.common.exception.IdempotencyException::class)
    fun handleIdempotencyException(ex: com.bank.system.common.exception.IdempotencyException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            code = ErrorCode.IDEMPOTENCY_CONFLICT.code,
            message = ex.message ?: ErrorCode.IDEMPOTENCY_CONFLICT.message
        )
        return ResponseEntity.status(ErrorCode.IDEMPOTENCY_CONFLICT.status).body(response)
    }
}