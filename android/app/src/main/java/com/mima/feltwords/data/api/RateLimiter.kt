package com.mima.feltwords.data.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 协程版限流器 —— 20 次/60 秒，语义对齐 iOS RequestRateLimiter。
 * 使用 Mutex + 时间戳队列实现线程安全。
 */
class RateLimiter(
    private val limit: Int = 20,
    private val intervalMs: Long = 60_000L
) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>()

    /** 等待直到有空位，再记录本次请求时间戳 */
    suspend fun waitForSlot() {
        while (true) {
            mutex.withLock {
                val now = System.currentTimeMillis()
                // 清理过期时间戳
                while (timestamps.isNotEmpty() && now - timestamps.first() >= intervalMs) {
                    timestamps.removeFirst()
                }
                if (timestamps.size < limit) {
                    timestamps.addLast(now)
                    return
                }
                // 需要等待：计算到最早记录过期的延迟
                val oldest = timestamps.first()
                val waitMs = (intervalMs - (now - oldest) + 200).coerceAtLeast(500)
                // 释放锁后再 delay
                delay(waitMs)
            }
        }
    }
}
