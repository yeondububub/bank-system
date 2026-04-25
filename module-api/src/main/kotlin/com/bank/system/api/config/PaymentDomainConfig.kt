package com.bank.system.api.config

import com.bank.system.domain.PaymentRepository
import com.bank.system.domain.PaymentService
import com.bank.system.domain.PgPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentDomainConfig {

    @Bean
    fun paymentService(
        paymentRepository: PaymentRepository,
        pgPort: PgPort
    ): PaymentService {
        return PaymentService(paymentRepository, pgPort)
    }
}