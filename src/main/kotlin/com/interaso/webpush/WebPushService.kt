package com.interaso.webpush

import kotlinx.coroutines.future.await
import java.net.*
import java.net.http.*
import java.net.http.HttpResponse.*

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
    public fun send(
        payload: String,
        endpoint: String,
        p256dh: String,
        auth: String,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        return send(payload.toByteArray(), endpoint, decodeBase64(p256dh), decodeBase64(auth), ttl, topic, urgency)
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
    public fun send(
        payload: ByteArray,
        endpoint: String,
        p256dh: ByteArray,
        auth: ByteArray,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        val request = getRequest(payload, endpoint, p256dh, auth, ttl, topic, urgency)
        val response = httpClient.send(request, BodyHandlers.ofString())
        return getSubscriptionState(response)
    }

    /**
     * Asynchronously sends a push notification using the given endpoint and credentials.
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
    public suspend fun sendAsync(
        payload: String,
        endpoint: String,
        p256dh: String,
        auth: String,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        return sendAsync(payload.toByteArray(), endpoint, decodeBase64(p256dh), decodeBase64(auth), ttl, topic, urgency)
    }

    /**
     * Asynchronously sends a push notification using the given endpoint and credentials.
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
    public suspend fun sendAsync(
        payload: ByteArray,
        endpoint: String,
        p256dh: ByteArray,
        auth: ByteArray,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): WebPush.SubscriptionState {
        val request = getRequest(payload, endpoint, p256dh, auth, ttl, topic, urgency)
        val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
        return getSubscriptionState(response)
    }

    private fun getRequest(
        payload: ByteArray,
        endpoint: String,
        p256dh: ByteArray,
        auth: ByteArray,
        ttl: Int?,
        topic: String?,
        urgency: WebPush.Urgency?,
    ) : HttpRequest {
        val body = webPush.getBody(payload, p256dh, auth)
        val headers = webPush.getHeaders(endpoint, ttl, topic, urgency)

        return HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .uri(URI.create(endpoint))
            .apply { headers.forEach { setHeader(it.key, it.value) } }
            .build()
    }

    private fun getSubscriptionState(response: HttpResponse<String>) =
        webPush.getSubscriptionState(
            response.statusCode(),
            response.body(),
        )
}