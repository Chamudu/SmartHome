package com.smarthome.app.data.recovery

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/**
 * Classifies Firestore/Auth failures so the client can choose a recovery path.
 *
 * Recoverable failures are transient (temporary connectivity or server throttling) and should be
 * retried with backoff or queued until connectivity returns. Session-revoked failures are explicit
 * authentication failures; a Firestore permission denial is an authorization/data-contract failure
 * and must not destroy an otherwise valid login session.
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
        else -> false
    }

    private val recoverableCodeNames = setOf(
        "UNAVAILABLE",
        "ABORTED",
        "RESOURCE_EXHAUSTED",
        "DEADLINE_EXCEEDED",
    )
}

/**
 * Marks a request that the server rejected because the authenticated session is no longer valid.
 * Thrown by client code (or injected in tests) instead of relying on a Firestore error type.
 */
class SessionRevokedException(
    message: String,
) : IllegalStateException(message)
