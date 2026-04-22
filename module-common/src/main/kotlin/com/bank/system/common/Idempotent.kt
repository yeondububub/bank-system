package com.bank.system.common

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent(
    val headerName: String = "Idempotency-Key",
    val ttlSeconds: Long = 86400L // 24시간
)
