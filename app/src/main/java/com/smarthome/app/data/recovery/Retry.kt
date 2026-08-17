package com.smarthome.app.data.recovery

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.pow

/**
 * Re-subscribes to a cold [Flow] after a recoverable failure, waiting between attempts so the
 * upstream listener is registered again once connectivity has a chance to return.
 *
 * Non-recoverable failures are re-thrown unchanged so callers can surface them or route them to a
 * session-revoked handler.
 */
fun <T> Flow<T>.retryWithBackoff(
    isRetryable: (Throwable) -> Boolean,
    onRetry: suspend (cause: Throwable, attempt: Int) -> Unit = { _, _ -> },
    baseDelayMillis: Long = 1_000L,
    maxDelayMillis: Long = 30_000L,
    maxRetries: Int = Int.MAX_VALUE,
): Flow<T> = flow {
    var attempt = 0
    while (true) {
        try {
            this@retryWithBackoff.collect { value -> emit(value) }
            break
        } catch (cause: Throwable) {
            if (!isRetryable(cause) || attempt >= maxRetries) throw cause
            attempt += 1
            onRetry(cause, attempt)
            val delayMillis = (baseDelayMillis * 2.0.pow(attempt - 1)).toLong()
                .coerceAtMost(maxDelayMillis)
            delay(delayMillis)
        }
    }
}

fun commandBackoffMillis(
    attempt: Int,
    baseDelayMillis: Long = 1_000L,
    maxDelayMillis: Long = 30_000L,
): Long = (baseDelayMillis * 2.0.pow(attempt - 1)).toLong().coerceAtMost(maxDelayMillis)