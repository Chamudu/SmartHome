package com.smarthome.app.data.recovery

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/**
 * Classifies Firestore/Auth failures so the client can choose a recovery path.
 *
 * Recoverable failures are transient (temporary connectivity or server throttling) and should be
 * retried with backoff or queued until connectivity returns. Session-revoked failures mean the
 * authenticated session is no longer accepted by the server and the user must sign in again.
 *
 * The classifier deliberately avoids touching [FirebaseFirestoreException]'s static state unless the
 * cause actually is a Firestore exception, because that class's static initializer depends on the
 * Android runtime and must not run in a plain JVM unit test.
 */
object FirestoreErrorClassifier {

    fun isRecoverable(cause: Throwable): Boolean = when (cause) {
        is SessionRevokedException -> false
        is IOException -> true
        is FirebaseFirestoreException -> cause.code.name in recoverableCodeNames
        else -> false
    }

    fun isSessionRevoked(cause: Throwable): Boolean = when (cause) {
        is SessionRevokedException -> true
        is FirebaseFirestoreException -> cause.code.name == PERMISSION_DENIED_NAME
        else -> false
    }

    private val recoverableCodeNames = setOf(
        "UNAVAILABLE",
        "ABORTED",
        "RESOURCE_EXHAUSTED",
        "DEADLINE_EXCEEDED",
    )

    private const val PERMISSION_DENIED_NAME = "PERMISSION_DENIED"
}

/**
 * Marks a request that the server rejected because the authenticated session is no longer valid.
 * Thrown by client code (or injected in tests) instead of relying on a Firestore error type.
 */
class SessionRevokedException(
    message: String,
) : IllegalStateException(message)