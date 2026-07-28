package com.bank.system.common.exception

enum class ErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {
    INVALID_REQUEST(400, "INVALID_REQUEST", "잘못된 요청입니다."),
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    UNAPPROVED_ACCOUNT(403, "UNAPPROVED_ACCOUNT", "승인 완료된 활성 계좌만 거래할 수 있습니다."),
    ACCOUNT_NOT_FOUND(404, "ACCOUNT_NOT_FOUND", "계좌 정보를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND(404, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."),
    PG_APPROVAL_FAILED(500, "PG_APPROVAL_FAILED", "PG사 결제 승인 요청에 실패했습니다."),
    EXTERNAL_SERVER_ERROR(503, "EXTERNAL_SERVER_ERROR", "외부 서비스 연동 중 오류가 발생했습니다."),
    IDEMPOTENCY_CONFLICT(409, "IDEMPOTENCY_CONFLICT", "요청이 이미 처리 중이거나 완료되었습니다."),
    
    // Auth & User Error Codes
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(400, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요하거나 유효하지 않은 토큰입니다."),
    FORBIDDEN(403, "FORBIDDEN", "관리자 전용 기능입니다. 접근 권한이 없습니다."),

    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부에서 오류가 발생했습니다.")
}
