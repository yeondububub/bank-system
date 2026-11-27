package com.bank.system.api.application

import com.bank.system.api.config.AuthenticatedUser
import com.bank.system.api.dto.TransferRequest
import com.bank.system.api.dto.TransferResponse
import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.TransactionType
import com.bank.system.domain.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AccountFacade(
    private val accountRepository: AccountRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val userService: UserService
) {

    @Transactional
    fun transferMoney(
        request: TransferRequest,
        loginUser: AuthenticatedUser
    ): TransferResponse {
        val fromCandidate = findAccountByNumberOrThrow(
            rawNumber = request.fromAccountNumber,
            notFoundMessage = "출금 계좌를 찾을 수 없습니다. (AccountNumber: ${request.fromAccountNumber})"
        )

        if (!loginUser.isAdmin() && loginUser.userId != fromCandidate.ownerId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED)
        }

        val toCandidate = findAccountByNumberOrThrow(
            rawNumber = request.toAccountNumber,
            notFoundMessage = "입금 상대방 계좌번호(${request.toAccountNumber})를 찾을 수 없습니다."
        )

        val fromId = requireNotNull(fromCandidate.id) { "출금 계좌 ID가 존재하지 않습니다." }
        val toId = requireNotNull(toCandidate.id) { "입금 계좌 ID가 존재하지 않습니다." }

        if (fromId == toId || fromCandidate.accountNumber == toCandidate.accountNumber) {
            throw IllegalArgumentException("동일한 계좌로는 송금할 수 없습니다.")
        }

        // 교착 상태(Deadlock) 방지를 위해 항상 계좌 ID 오름차순으로 비관적 쓰기 락(FOR UPDATE) 획득
        val firstLockId = minOf(fromId, toId)
        val secondLockId = maxOf(fromId, toId)

        val firstLocked = accountRepository.findByIdWithLock(firstLockId) ?: firstCandidate(firstLockId, fromCandidate, toCandidate)
        val secondLocked = accountRepository.findByIdWithLock(secondLockId) ?: firstCandidate(secondLockId, fromCandidate, toCandidate)

        val fromAccount = if (fromId == firstLockId) firstLocked else secondLocked
        val toAccount = if (toId == firstLockId) firstLocked else secondLocked

        fromAccount.withdraw(request.amount)
        toAccount.deposit(request.amount)

        accountRepository.save(fromAccount)
        accountRepository.save(toAccount)

        val fromUser = userService.getById(fromAccount.ownerId)
        val toUser = userService.getById(toAccount.ownerId)

        accountTransactionRepository.save(
            AccountTransaction(
                accountNumber = fromAccount.accountNumber,
                type = TransactionType.TRANSFER_OUT,
                amount = request.amount,
                balanceAfter = fromAccount.balance,
                counterpartyName = toUser?.name ?: "고객",
                counterpartyAccountNumber = toAccount.accountNumber,
                memo = request.memo
            )
        )

        accountTransactionRepository.save(
            AccountTransaction(
                accountNumber = toAccount.accountNumber,
                type = TransactionType.TRANSFER_IN,
                amount = request.amount,
                balanceAfter = toAccount.balance,
                counterpartyName = fromUser?.name ?: "고객",
                counterpartyAccountNumber = fromAccount.accountNumber,
                memo = request.memo
            )
        )

        return TransferResponse(
            transactionId = snowflakeIdGenerator.nextId().toString(),
            fromAccountId = fromId,
            fromAccountNumber = fromAccount.accountNumber,
            toAccountNumber = toAccount.accountNumber,
            amount = request.amount,
            balanceAfterFrom = fromAccount.balance,
            timestamp = LocalDateTime.now().toString()
        )
    }

    private fun findAccountByNumberOrThrow(rawNumber: String, notFoundMessage: String): Account {
        val trimmed = rawNumber.trim()
        val cleanNum = trimmed.replace("-", "")
        return accountRepository.findByAccountNumber(trimmed)
            ?: accountRepository.findByAccountNumber(cleanNum)
            ?: throw IllegalArgumentException(notFoundMessage)
    }

    private fun firstCandidate(targetId: Long, a: Account, b: Account): Account {
        return if (a.id == targetId) a else b
    }
}
