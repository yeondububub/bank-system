package com.bank.system.domain.exception

import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode

class DuplicateEmailException(email: String) : BusinessException(
    ErrorCode.DUPLICATE_EMAIL,
    "이미 등록된 이메일입니다. (email: $email)"
)

class UserNotFoundException(identifier: String) : BusinessException(
    ErrorCode.USER_NOT_FOUND,
    "사용자를 찾을 수 없습니다. ($identifier)"
)

class InvalidPasswordException : BusinessException(
    ErrorCode.INVALID_PASSWORD
)
