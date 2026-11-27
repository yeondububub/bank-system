package com.bank.system.api.controller

import com.bank.system.api.application.AccountFacade
import com.bank.system.api.config.AuthenticatedUser
import com.bank.system.api.config.LoginUser
import com.bank.system.api.dto.AccountHolderResponse
import com.bank.system.api.dto.AccountResponse
import com.bank.system.api.dto.AccountTransactionResponse
import com.bank.system.api.dto.CreateAccountRequest
import com.bank.system.api.dto.TransferRequest
import com.bank.system.api.dto.TransferResponse
import com.bank.system.common.Idempotent
import com.bank.system.common.exception.BusinessException
import com.bank.system.common.exception.ErrorCode
import com.bank.system.common.util.AccountNumberGenerator
import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.UserService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
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
    private val userService: UserService,
    private val accountFacade: AccountFacade = AccountFacade(
        accountRepository = accountRepository,
        accountTransactionRepository = accountTransactionRepository,
        snowflakeIdGenerator = snowflakeIdGenerator,
        userService = userService
    ),
    @Value("\${account.initial-balance:1000000}")
    private val defaultInitialBalance: Long = 1_000_000L
) {

    /**
     * 유저의 대표(메인) 계좌 조회
     */
    @GetMapping("/{ownerId}")
    fun getPrimaryAccount(
        @PathVariable ownerId: Long,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<AccountResponse> {
        validateOwnership(ownerId, loginUser)

        val account = accountRepository.findPrimaryByOwnerId(ownerId)
            ?: accountRepository.findAllByOwnerId(ownerId).firstOrNull()
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(AccountResponse.from(account))
    }

    /**
     * 유저의 모든 다계좌 목록 조회
     */
    @GetMapping("/user/{ownerId}")
    fun getUserAccounts(
        @PathVariable ownerId: Long,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<List<AccountResponse>> {
        validateOwnership(ownerId, loginUser)

        val accounts = accountRepository.findAllByOwnerId(ownerId)
        return ResponseEntity.ok(accounts.map { AccountResponse.from(it) })
    }

    /**
     * 계좌 신규 개설 (1인 다계좌 허용)
     */
    @PostMapping
    @Transactional
    fun createAccount(
        @LoginUser loginUser: AuthenticatedUser,
        @RequestParam(required = false) ownerId: Long? = null,
        @RequestBody(required = false) request: CreateAccountRequest? = null
    ): ResponseEntity<AccountResponse> {
        val requestedOwnerId = request?.ownerId ?: ownerId
        if (requestedOwnerId != null) {
            validateOwnership(requestedOwnerId, loginUser)
        }
        val finalOwnerId = requestedOwnerId ?: loginUser.userId

        val existingAccounts = accountRepository.findAllByOwnerId(finalOwnerId)
        val isFirstAccount = existingAccounts.isEmpty()

        var accountNumber = AccountNumberGenerator.generate()
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = AccountNumberGenerator.generate()
        }

        val account = Account(
            id = snowflakeIdGenerator.nextId(),
            ownerId = finalOwnerId,
            accountNumber = accountNumber,
            balance = defaultInitialBalance,
            isPrimary = isFirstAccount
        )
        val savedAccount = accountRepository.save(account)

        return ResponseEntity.ok(AccountResponse.from(savedAccount))
    }

    /**
     * 메인(대표) 계좌 변경 API
     */
    @PostMapping("/{accountId}/primary")
    @Transactional
    fun setPrimaryAccount(
        @PathVariable accountId: Long,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<AccountResponse> {
        val targetAccount = accountRepository.findById(accountId)
            ?: return ResponseEntity.notFound().build()

        validateOwnership(targetAccount.ownerId, loginUser)

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
     * 계좌 간 실시간 송금 API (멱등성 및 계좌 ID 오름차순 비관적 락 적용)
     */
    @Idempotent
    @PostMapping("/transfer")
    fun transferMoney(
        @Valid @RequestBody request: TransferRequest,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<TransferResponse> {
        val response = accountFacade.transferMoney(request, loginUser)
        return ResponseEntity.ok(response)
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
    fun getAccountTransactions(
        @PathVariable accountNumber: String,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<List<AccountTransactionResponse>> {
        val cleanNum = accountNumber.replace("-", "").trim()
        val account = accountRepository.findByAccountNumber(accountNumber.trim())
            ?: accountRepository.findByAccountNumber(cleanNum)
            ?: throw BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)

        validateOwnership(account.ownerId, loginUser)

        val list = accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(account.accountNumber)
        return ResponseEntity.ok(list.map { AccountTransactionResponse.from(it) })
    }

    /**
     * 특정 유저가 보유한 계좌 거래 내역 목록 조회 API
     */
    @GetMapping("/user/{userId}/transactions")
    fun getUserTransactions(
        @PathVariable userId: Long,
        @RequestParam(required = false) accountNumber: String?,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<List<AccountTransactionResponse>> {
        validateOwnership(userId, loginUser)

        if (!accountNumber.isNullOrBlank()) {
            val cleanNum = accountNumber.replace("-", "").trim()
            val account = accountRepository.findByAccountNumber(accountNumber.trim())
                ?: accountRepository.findByAccountNumber(cleanNum)
                ?: throw BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)

            if (account.ownerId != userId) {
                throw BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "해당 사용자의 계좌가 아닙니다."
                )
            }

            val list = accountTransactionRepository.findAllByAccountNumberOrderByCreatedAtDesc(account.accountNumber)
            return ResponseEntity.ok(list.map { AccountTransactionResponse.from(it) })
        }

        val userAccounts = accountRepository.findAllByOwnerId(userId)
        val accountNums = userAccounts.map { it.accountNumber }
        val list = accountTransactionRepository.findAllByAccountNumberInOrderByCreatedAtDesc(accountNums)
        return ResponseEntity.ok(list.map { AccountTransactionResponse.from(it) })
    }

    /**
     * 특정 유저의 특정 계좌 거래 내역 목록 조회 API (RESTful 계층형 엔드포인트)
     */
    @GetMapping("/user/{userId}/accounts/{accountNumber}/transactions")
    fun getUserSpecificAccountTransactions(
        @PathVariable userId: Long,
        @PathVariable accountNumber: String,
        @LoginUser loginUser: AuthenticatedUser
    ): ResponseEntity<List<AccountTransactionResponse>> {
        return getUserTransactions(userId, accountNumber, loginUser)
    }

    private fun validateOwnership(targetOwnerId: Long, loginUser: AuthenticatedUser) {
        if (!loginUser.isAdmin() && loginUser.userId != targetOwnerId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED)
        }
    }
}
