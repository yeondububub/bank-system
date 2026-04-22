package com.bank.system.api.controller

import com.bank.system.api.application.PaymentFacade
import com.bank.system.api.dto.PaymentRequest
import com.bank.system.api.dto.PaymentResponse
import com.bank.system.common.Idempotent
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentFacade: PaymentFacade
) {
    @Idempotent
    @PostMapping
    fun createPayment(@Valid @RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        val payment = paymentFacade.createPayment(
            orderId = request.orderId,
            buyerId = request.buyerId,
            amount = request.amount
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(PaymentResponse.from(payment))
    }

    @Idempotent
    @PostMapping("/{orderId}/approve")
    fun approvePayment(@PathVariable orderId: String): ResponseEntity<PaymentResponse> {
        val approvedPayment = paymentFacade.approvePayment(orderId)

        return ResponseEntity.ok(PaymentResponse.from(approvedPayment))
    }
}