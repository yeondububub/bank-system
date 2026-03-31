package com.bank.system.api.exception

import com.bakn.system.common.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "잘못된 요청입니다."

        val errorResponse = ErrorResponse(
            code = "INVALID_REQUEST",
            message = errorMessage
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBusinessExceptions(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse (
            code = "BUSINESS_RULE_VIOLATION",
            message = ex.message ?: "비즈니스 로직 오류가 발생했습니다."
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        ex.printStackTrace()

        val response = ErrorResponse(
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 내부에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response)
    }


}