package com.bank.system.api.config

import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentDomainConfig {

    @Bean
    fun paymentService(paymentRepository: PaymentRepository): PaymentService {
        return PaymentService(paymentRepository)
    }
}