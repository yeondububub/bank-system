package com.bank.system.domain.exception

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode

class InvalidRequestException(
    message: String = ErrorCode.INVALID_REQUEST.message
) : BusinessException(ErrorCode.INVALID_REQUEST, message)
