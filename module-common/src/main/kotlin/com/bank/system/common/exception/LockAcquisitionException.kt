package com.bank.system.common.exception

class LockAcquisitionException(
    message: String = ErrorCode.LOCK_ACQUISITION_FAILED.message
) : BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED, message)
