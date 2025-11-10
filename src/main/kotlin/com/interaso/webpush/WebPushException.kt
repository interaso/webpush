package com.interaso.webpush

/**
 * Represents an exception that occurs during web push operations.
 *
 * @param message The detail message of the exception.
 * @param cause The cause of the exception, or null if the cause is nonexistent or unknown.
 */
public open class WebPushException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
