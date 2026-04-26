package com.bank.system.domain.exception

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode

class InsufficientBalanceException(
    message: String = ErrorCode.INSUFFICIENT_BALANCE.message
) : BusinessException(ErrorCode.INSUFFICIENT_BALANCE, message)
