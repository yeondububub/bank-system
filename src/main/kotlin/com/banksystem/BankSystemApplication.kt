package com.banksystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankSystemApplication

fun main(args: Array<String>) {
    runApplication<BankSystemApplication>(*args)
}
