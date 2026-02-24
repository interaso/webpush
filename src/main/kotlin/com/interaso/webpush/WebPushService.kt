package com.interaso.webpush

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * Represents a service for sending web push notifications.
 *
 * @property subject The subject identifying the push notification sender. It must start with "mailto:" or "https://".
 * @property vapidKeys The VapidKeys object containing the public and private keys for VAPID authentication.
 */
public class WebPushService(
    public val subject: String,
    public val vapidKeys: VapidKeys,
) {
    private val webPush = WebPush(subject, vapidKeys)
    private val httpClient = HttpClient.newBuilder().build()

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
     * @throws WebPushStatusException if an unexpected status code is received from the push service.
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
     * @throws WebPushStatusException if an unexpected status code is received from the push service.
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
     * @throws WebPushStatusException if an unexpected status code is received from the push service.
     * @throws WebPushException if an unexpected exception is caught while constructing request.
     */
    public fun send(notification: Notification): WebPush.SubscriptionState {
        val body = webPush.getBody(notification.payload, notification.p256dh, notification.auth)
        val headers = webPush.getHeaders(notification.endpoint, notification.ttl, notification.topic, notification.urgency)

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .uri(URI.create(notification.endpoint))
            .apply { headers.forEach { setHeader(it.key, it.value) } }
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())

        return webPush.getSubscriptionState(
            response.statusCode(),
            response.body(),
        )
    }
}
