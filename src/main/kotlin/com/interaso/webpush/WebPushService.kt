package com.interaso.webpush

import java.net.*
import java.net.http.*
import java.net.http.HttpResponse.*

/**
 * Represents a service for sending web push notifications.
 */
public abstract class WebPushService(protected val webPush: WebPush) {

    /**
     * The subject identifying the push notification sender. It must start with "mailto:" or "https://".
     */
    public val subject: String
        get() = webPush.subject

    /**
     * The VapidKeys object containing the public and private keys for VAPID authentication.
     */
    public val vapidKeys: VapidKeys
        get() = webPush.vapidKeys

    /**
     * Sends a push notification using the given endpoint and credentials.
     *
     * @param payload The message payload to be sent in the push notification.
     * @param endpoint The URL endpoint that identifies the push service subscription.
     * @param p256dh The Base64-encoded P256DH key for authentication with the push service provider.
     * @param auth The Base64-encoded authentication secret for the push service provider.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     *
     * @return current state of this subscription
     * @throws WebPushException if an unexpected exception is caught while constructing request.
     */
    @Deprecated(message = "Streamlined WebPushService API using a single send method with a Notification object",
        ReplaceWith(
            expression = "send(Notification(payload, endpoint, p256dh, auth, ttl, topic, urgency))",
            imports    = ["com.interaso.webpush.Notification"]
        )
    )
    public fun send(
        payload: String,
        endpoint: String,
        p256dh: String,
        auth: String,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        return send(Notification(payload, endpoint, p256dh, auth, ttl, topic, urgency))
    }

    /**
     * Sends a push notification using the given endpoint and credentials.
     *
     * @param payload The message payload to be sent in the push notification.
     * @param endpoint The URL endpoint that identifies the push service subscription.
     * @param p256dh The P256DH key for authentication with the push service provider.
     * @param auth The authentication secret for the push service provider.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     *
     * @return current state of this subscription
     * @throws WebPushException if an unexpected exception is caught while constructing request.
     */
    @Deprecated(message = "Streamlined WebPushService API using a single send method with a Notification object",
        ReplaceWith(
            expression = "send(Notification(payload, endpoint, p256dh, auth, ttl, topic, urgency))",
            imports    = ["com.interaso.webpush.Notification"]
        )
    )
    public fun send(
        payload: ByteArray,
        endpoint: String,
        p256dh: ByteArray,
        auth: ByteArray,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        return send(Notification(payload, endpoint, p256dh, auth, ttl, topic, urgency))
    }

    /**
     * Sends a push notification using the given endpoint and credentials.
     *
     * @param notification The web push notification to be sent.
     *
     * @return current state of this subscription
     * @throws WebPushException if an unexpected exception is caught while constructing request.
     */
    public abstract fun send(notification: Notification): WebPush.SubscriptionState
}

/**
 * Represents a service for sending web push notifications using the built-in JDK [HttpClient].
 */
public class JdkHttpClientWebPushService(
    webPush: WebPush,
    httpClient: HttpClient? = null
): WebPushService(webPush) {

    private val httpClient: HttpClient = httpClient ?: HttpClient.newHttpClient()

    public constructor(
        subject: String,
        vapidKeys: VapidKeys,
        httpClient: HttpClient? = null
    ) : this(WebPush(subject, vapidKeys), httpClient)

    public override fun send(notification: Notification): WebPush.SubscriptionState {
        val body = webPush.getBody(notification.payload, notification.p256dh, notification.auth)
        val headers = webPush.getHeaders(notification.endpoint, notification.ttl, notification.topic, notification.urgency)

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .uri(URI.create(notification.endpoint))
            .apply { headers.forEach { setHeader(it.key, it.value) } }
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())

        try {
            return webPush.getSubscriptionState(
                response.statusCode(),
                response.body(),
            )
        } catch (exception: WebPushException) {
            throw WebPushHttpException(
                response,
                "Received a response with an unsuccessful HTTP status code: [${response.statusCode()}] - ${response.body()}",
                exception
            )
        }
    }

    /**
     * Represents an exception that occurs during web push sending.
     *
     * @param response The HTTP response object which caused this exception.
     * @param message The detail message of the exception.
     * @param cause The cause of the exception, or null if the cause is nonexistent or unknown.
     */
    public class WebPushHttpException(
        public val response: HttpResponse<String>,
        message: String,
        cause: Throwable
    ) : WebPushException(message, cause)
}
