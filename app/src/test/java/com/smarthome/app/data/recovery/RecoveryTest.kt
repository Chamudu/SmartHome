package com.smarthome.app.data.recovery

import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreErrorClassifierTest {

    @Test
    fun `ioexception is recoverable`() {
        assertTrue(FirestoreErrorClassifier.isRecoverable(IOException("Network is unreachable.")))
    }

    @Test
    fun `session revoked marker is session revoked but not recoverable`() {
        val cause = SessionRevokedException("Session expired.")

        assertTrue(FirestoreErrorClassifier.isSessionRevoked(cause))
        assertFalse(FirestoreErrorClassifier.isRecoverable(cause))
    }

    @Test
    fun `unknown failure is not recoverable and not session revoked`() {
        val cause = IllegalStateException("Unexpected.")

        assertFalse(FirestoreErrorClassifier.isRecoverable(cause))
        assertFalse(FirestoreErrorClassifier.isSessionRevoked(cause))
    }
}

class RetryTest {

    @Test
    fun `retryWithBackoff resubscribes after transient failure`() = runTest {
        var attempts = 0
        val source = flow {
            attempts += 1
            if (attempts == 1) {
                throw IOException("Service unavailable.")
            }
            emit(1)
            emit(2)
        }

        val retries = mutableListOf<Int>()
        val result = source
            .retryWithBackoff(
                isRetryable = { cause -> cause is IOException },
                onRetry = { _, attempt -> retries += attempt },
                baseDelayMillis = 100L,
            )
            .toList()

        assertEquals(listOf(1, 2), result)
        assertEquals(listOf(1), retries)
    }

    @Test
    fun `retryWithBackoff propagates non-retryable failure`() = runTest {
        val source = flow<Int> { throw IllegalStateException("terminal") }

        val error = runCatching {
            source.retryWithBackoff(isRetryable = { false }).toList()
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("terminal", error?.message)
    }

    @Test
    fun `command backoff grows then caps at maximum delay`() {
        assertEquals(1_000L, commandBackoffMillis(1))
        assertEquals(2_000L, commandBackoffMillis(2))
        assertEquals(30_000L, commandBackoffMillis(10))
    }
}
