package com.interaso.webpush

/**
 * Represents an exception that occurs during web push operations.
 *
 * @param message The detail message of the exception.
 * @param cause The cause of the exception, or null if the cause is nonexistent or unknown.
 */
public sealed class WebPushException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Represents an exception that occurs during web push sending.
 *
 * @param statusCode The HTTP status code returned by server
 * @param message The detail message of the exception.
 * @param cause The cause of the exception, or null if the cause is nonexistent or unknown.
 */
public class WebPushStatusException(
    public val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : WebPushException(message, cause)
