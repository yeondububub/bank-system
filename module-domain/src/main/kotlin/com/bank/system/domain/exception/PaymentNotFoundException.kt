package com.bank.system.domain.exception

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode

class PaymentNotFoundException(
    orderId: String
) : BusinessException(
    errorCode = ErrorCode.PAYMENT_NOT_FOUND,
    message = "결제 정보를 찾을 수 없습니다. (orderId: $orderId)"
)
