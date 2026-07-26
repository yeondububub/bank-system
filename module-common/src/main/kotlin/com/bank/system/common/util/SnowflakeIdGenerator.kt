package com.bank.system.common.util

class SnowflakeIdGenerator(
    private val workerId: Long = 1L,
    private val datacenterId: Long = 1L
) {
    companion object {
        private const val EPOCH = 1704067200000L // 2024-01-01 00:00:00 UTC

        private const val WORKER_ID_BITS = 5L
        private const val DATACENTER_ID_BITS = 5L
        private const val SEQUENCE_BITS = 12L

        private const val MAX_WORKER_ID = -1L xor (-1L shl WORKER_ID_BITS.toInt())
        private const val MAX_DATACENTER_ID = -1L xor (-1L shl DATACENTER_ID_BITS.toInt())
        private const val MAX_SEQUENCE = -1L xor (-1L shl SEQUENCE_BITS.toInt())

        private const val WORKER_ID_SHIFT = SEQUENCE_BITS
        private const val DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS
        private const val TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS
    }

    private var sequence = 0L
    private var lastTimestamp = -1L

    init {
        require(workerId in 0..MAX_WORKER_ID) {
            "Worker ID는 0과 $MAX_WORKER_ID 사이여야 합니다."
        }
        require(datacenterId in 0..MAX_DATACENTER_ID) {
            "Datacenter ID는 0과 $MAX_DATACENTER_ID 사이여야 합니다."
        }
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp = timeGen()

        if (timestamp < lastTimestamp) {
            throw IllegalStateException("NTP문제발생! ${lastTimestamp - timestamp}ms 동안 ID 생성이 거부됩니다.")
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return ((timestamp - EPOCH) shl TIMESTAMP_LEFT_SHIFT.toInt()) or
                (datacenterId shl DATACENTER_ID_SHIFT.toInt()) or
                (workerId shl WORKER_ID_SHIFT.toInt()) or
                sequence
    }

    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = timeGen()
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen()
        }
        return timestamp
    }

    private fun timeGen(): Long = System.currentTimeMillis()
}
