package com.bank.system.domain.exception

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode

class PgApprovalException(
    message: String = ErrorCode.PG_APPROVAL_FAILED.message
) : BusinessException(ErrorCode.PG_APPROVAL_FAILED, message)
