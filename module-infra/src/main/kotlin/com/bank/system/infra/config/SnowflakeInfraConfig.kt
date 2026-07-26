package com.bank.system.infra.config

import com.bank.system.common.util.SnowflakeIdGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SnowflakeInfraConfig {
    @Bean
    @ConditionalOnMissingBean
    fun snowflakeIdGenerator(): SnowflakeIdGenerator {
        return SnowflakeIdGenerator(workerId = 1L, datacenterId = 1L)
    }
}
