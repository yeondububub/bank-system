package com.bank.system.api.controller

import com.bank.system.api.application.PaymentFacade
import com.bank.system.api.dto.PaymentRequest
import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentStatus
import com.bank.system.domain.exception.PaymentNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(PaymentController::class)
class PaymentControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockkBean
    private lateinit var paymentFacade: PaymentFacade

    @Test
    @DisplayName("결제 생성 요청이 유효하면 201 Created를 반환한다")
    fun createPaymentSuccess() {
        val request = PaymentRequest(
            orderId = "ORD-12345",
            buyerId = 1004L,
            amount = 50000L
        )
        val expectedPayment = Payment(
            id = 1L,
            orderId = request.orderId,
            buyerId = request.buyerId,
            amount = request.amount,
            status = PaymentStatus.PENDING
        )

        every {
            paymentFacade.createPayment(request.orderId, request.buyerId, request.amount)
        } returns expectedPayment

        mockMvc.post("/api/v1/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.paymentId") { value(1L) }
            jsonPath("$.orderId") { value(request.orderId) }
            jsonPath("$.amount") { value(request.amount) }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    @DisplayName("결제 금액이 100원 미만이면 400 Bad Request를 반환한다")
    fun createPaymentValidationFailure() {
        val request = PaymentRequest(
            orderId = "ORD-12345",
            buyerId = 1004L,
            amount = 50L // Min value is 100
        )

        mockMvc.post("/api/v1/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
            jsonPath("$.message") { value("최소 결제 금액은 100원 입니다.") }
        }
    }

    @Test
    @DisplayName("결제 승인이 성공하면 200 OK를 반환한다")
    fun approvePaymentSuccess() {
        val orderId = "ORD-12345"
        val expectedPayment = Payment(
            id = 1L,
            orderId = orderId,
            buyerId = 1004L,
            amount = 50000L,
            status = PaymentStatus.SUCCESS
        )

        every { paymentFacade.approvePayment(orderId) } returns expectedPayment

        mockMvc.post("/api/v1/payments/$orderId/approve") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("SUCCESS") }
        }
    }

    @Test
    @DisplayName("결제 승인 대상 주문을 찾을 수 없으면 404 Not Found를 반환한다")
    fun approvePaymentNotFound() {
        val orderId = "ORD-NOT-FOUND"

        every { paymentFacade.approvePayment(orderId) } throws PaymentNotFoundException(orderId)

        mockMvc.post("/api/v1/payments/$orderId/approve") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("PAYMENT_NOT_FOUND") }
        }
    }

    @Test
    @DisplayName("결제 취소가 성공하면 200 OK를 반환한다")
    fun cancelPaymentSuccess() {
        val orderId = "ORD-12345"
        val expectedPayment = Payment(
            id = 1L,
            orderId = orderId,
            buyerId = 1004L,
            amount = 50000L,
            status = PaymentStatus.CANCELLED
        )

        every { paymentFacade.cancelPayment(orderId) } returns expectedPayment

        mockMvc.post("/api/v1/payments/$orderId/cancel") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
        }
    }
}
