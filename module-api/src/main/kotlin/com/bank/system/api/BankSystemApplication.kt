package com.bank.system.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(scanBasePackages = ["com.bank.system"])
class BankSystemApplication

fun main(args: Array<String>) {
    runApplication<BankSystemApplication>(*args)
}