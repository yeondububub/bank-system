package com.bank.system.infra.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["com.bank.system.infra"])
@EntityScan(basePackages = ["com.bank.system.infra"])
class JpaConfig {
}