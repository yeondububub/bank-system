package com.bank.system.common

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
        val key: String,
        val waitTime: Long = 5L,
        val leaseTime: Long = 3L,
        val timeUnit: TimeUnit = TimeUnit.SECONDS
)
