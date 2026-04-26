package com.bank.system.common.exception

enum class ErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {
    INVALID_REQUEST(400, "INVALID_REQUEST", "잘못된 요청입니다."),
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    PAYMENT_NOT_FOUND(404, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."),
    PG_APPROVAL_FAILED(500, "PG_APPROVAL_FAILED", "PG사 결제 승인 요청에 실패했습니다."),
    EXTERNAL_SERVER_ERROR(503, "EXTERNAL_SERVER_ERROR", "외부 서비스 연동 중 오류가 발생했습니다."),
    IDEMPOTENCY_CONFLICT(409, "IDEMPOTENCY_CONFLICT", "요청이 이미 처리 중이거나 완료되었습니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부에서 오류가 발생했습니다.")
}
