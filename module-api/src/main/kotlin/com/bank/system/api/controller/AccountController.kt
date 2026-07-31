package com.bank.system.api.controller

import com.bank.system.api.dto.AccountHolderResponse
import com.bank.system.api.dto.AccountResponse
import com.bank.system.api.dto.AccountTransactionResponse
import com.bank.system.api.dto.CreateAccountRequest
import com.bank.system.api.dto.TransferRequest
import com.bank.system.api.dto.TransferResponse
import com.bank.system.common.util.AccountNumberGenerator
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.TransactionType
import com.bank.system.domain.UserService
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val userService: UserService
) {

    /**
     * 유저의 대표(메인) 계좌 조회
     */
    @GetMapping("/{ownerId}")
    fun getPrimaryAccount(@PathVariable ownerId: Long): ResponseEntity<AccountResponse> {
        val account = accountRepository.findPrimaryByOwnerId(ownerId)
            ?: accountRepository.findAllByOwnerId(ownerId).firstOrNull()
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(AccountResponse.from(account))
    }

    /**
     * 유저의 모든 다계좌 목록 조회
     */
    @GetMapping("/user/{ownerId}")
    fun getUserAccounts(@PathVariable ownerId: Long): ResponseEntity<List<AccountResponse>> {
        val accounts = accountRepository.findAllByOwnerId(ownerId)
        return ResponseEntity.ok(accounts.map { AccountResponse.from(it) })
    }

    /**
     * 계좌 신규 개설 (1인 다계좌 허용)
     */
    @PostMapping
    @Transactional
    fun createAccount(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) initialBalance: Long?,
        @RequestBody(required = false) request: CreateAccountRequest?
    ): ResponseEntity<AccountResponse> {
        val finalOwnerId = request?.ownerId ?: ownerId ?: throw IllegalArgumentException("ownerId는 필수입니다.")
        val finalBalance = request?.initialBalance ?: initialBalance ?: 1000000L

        val existingAccounts = accountRepository.findAllByOwnerId(finalOwnerId)
        val isFirstAccount = existingAccounts.isEmpty()

        // 중복 없는 계좌번호 채번
        var accountNumber = AccountNumberGenerator.generate()
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = AccountNumberGenerator.generate()
        }

        val account = Account(
            id = snowflakeIdGenerator.nextId(),
            ownerId = finalOwnerId,
            accountNumber = accountNumber,
            balance = finalBalance,
            isPrimary = isFirstAccount // 첫 계좌는 메인 계좌로 자동 설정
        )
        val savedAccount = accountRepository.save(account)

        return ResponseEntity.ok(AccountResponse.from(savedAccount))
    }

    /**
     * 메인(대표) 계좌 변경 API
     */
    @PostMapping("/{accountId}/primary")
    @Transactional
    fun setPrimaryAccount(@PathVariable accountId: Long): ResponseEntity<AccountResponse> {
        val targetAccount = accountRepository.findById(accountId)
            ?: return ResponseEntity.notFound().build()

        // 동일 유저의 모든 계좌를 순회하며 지정 계좌는 Primary(true), 그 외 계좌는 Secondary(false) 지정
        val userAccounts = accountRepository.findAllByOwnerId(targetAccount.ownerId)
        for (acc in userAccounts) {
            if (acc.id == accountId) {
                acc.makePrimary()
            } else {
                acc.makeSecondary()
            }
            accountRepository.save(acc)
        }

        val updated = accountRepository.findById(accountId) ?: targetAccount
        return ResponseEntity.ok(AccountResponse.from(updated))
    }

    /**
     * 계좌 간 실시간 송금 API
     */
    @PostMapping("/transfer")
    @Transactional
    fun transferMoney(@RequestBody request: TransferRequest): ResponseEntity<TransferResponse> {
        val cleanFromNum = request.fromAccountNumber.replace("-", "").trim()
        val fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber.trim())
            ?: accountRepository.findByAccountNumber(cleanFromNum)
            ?: throw IllegalArgumentException("출금 계좌를 찾을 수 없습니다. (AccountNumber: ${request.fromAccountNumber})")

        val cleanToNum = request.toAccountNumber.replace("-", "").trim()
        val toAccount = accountRepository.findByAccountNumber(request.toAccountNumber.trim())
            ?: accountRepository.findByAccountNumber(cleanToNum)
            ?: throw IllegalArgumentException("입금 상대방 계좌번호(${request.toAccountNumber})를 찾을 수 없습니다.")

        if (fromAccount.id == toAccount.id || fromAccount.accountNumber == toAccount.accountNumber) {
            throw IllegalArgumentException("동일한 계좌로는 송금할 수 없습니다.")
        }

        // 출금 및 입금 처리 (상태 및 잔액 부족 검증은 도메인 객체 내부에서 자동 수행)
        fromAccount.withdraw(request.amount)
        toAccount.deposit(request.amount)

        accountRepository.save(fromAccount)
        accountRepository.save(toAccount)

        // 거래 내역(History) 기록 생성
        val fromUser = userService.getById(fromAccount.ownerId)
        val toUser = userService.getById(toAccount.ownerId)

        // 출금자 이력 기록 (TRANSFER_OUT)
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

        // 입금자 이력 기록 (TRANSFER_IN)
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

        return ResponseEntity.ok(
            TransferResponse(
                transactionId = snowflakeIdGenerator.nextId().toString(),
                fromAccountId = fromAccount.id!!,
                fromAccountNumber = fromAccount.accountNumber,
                toAccountNumber = toAccount.accountNumber,
                amount = request.amount,
                balanceAfterFrom = fromAccount.balance,
                timestamp = java.time.LocalDateTime.now().toString()
            )
        )
    }

    /**
     * 계좌 예금주(소유자) 정보 조회 API
     */
    @GetMapping("/holder/{accountNumber}")
    fun getAccountHolder(@PathVariable accountNumber: String): ResponseEntity<AccountHolderResponse> {
        val cleanNum = accountNumber.replace("-", "").trim()
        val account = accountRepository.findByAccountNumber(accountNumber.trim())
            ?: accountRepository.findByAccountNumber(cleanNum)
            ?: return ResponseEntity.notFound().build()

        val user = userService.getById(account.ownerId)
        val ownerName = user?.name ?: "고객"

        return ResponseEntity.ok(
            AccountHolderResponse(
                accountNumber = account.accountNumber,
                ownerId = account.ownerId,
                ownerName = ownerName
            )
        )
    }

    /**
     * 특정 계좌의 거래 내역 목록 조회 API
     */
    @GetMapping("/{accountNumber}/transactions")
    fun getAccountTransactions(@PathVariable accountNumber: String): ResponseEntity<List<AccountTransactionResponse>> {
        val cleanNum = accountNumber.replace("-", "").trim()
        val targetNum = if (accountRepository.existsByAccountNumber(accountNumber.trim())) accountNumber.trim() else cleanNum
        val list = accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(targetNum)
        return ResponseEntity.ok(list.map { AccountTransactionResponse.from(it) })
    }

    /**
     * 특정 유저가 보유한 모든 계좌의 거래 내역 목록 조회 API
     */
    @GetMapping("/user/{userId}/transactions")
    fun getUserAllTransactions(@PathVariable userId: Long): ResponseEntity<List<AccountTransactionResponse>> {
        val userAccounts = accountRepository.findAllByOwnerId(userId)
        val accountNums = userAccounts.map { it.accountNumber }
        val list = accountTransactionRepository.findAllByAccountNumberInOrderByCreatedAtDesc(accountNums)
        return ResponseEntity.ok(list.map { AccountTransactionResponse.from(it) })
    }
}