package com.interaso.webpush

import java.net.*
import java.net.http.*
import java.net.http.HttpResponse.*

/**
 * Represents a service for sending web push notifications.
 *
 * @property subject the subject of the push service
 * @property vapidKeys the VapidKeys used for authentication
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
     * @param endpoint The URL endpoint that identifies the push service provider.
     * @param p256dh The Base64-encoded P256DH key for authentication with the push service provider.
     * @param auth The Base64-encoded authentication secret for the push service provider.
     * @param payload The message payload to be sent in the push notification.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     *
     * @return `true` if the push notification is sent successfully, `false` otherwise.
     * @throws RuntimeException if an unexpected status code is received from the push service.
     */
    public fun send(
        endpoint: String,
        p256dh: String,
        auth: String,
        payload: String,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): Boolean {
        return send(endpoint, decodeBase64(p256dh), decodeBase64(auth), payload.toByteArray(), ttl, topic, urgency)
    }

    /**
     * Sends a push notification using the given endpoint and credentials.
     *
     * @param endpoint The URL endpoint that identifies the push service provider.
     * @param p256dh The P256DH key for authentication with the push service provider.
     * @param auth The authentication secret for the push service provider.
     * @param payload The message payload to be sent in the push notification.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     *
     * @return `true` if the push notification is sent successfully, `false` otherwise.
     * @throws RuntimeException if an unexpected status code is received from the push service.
     */
    public fun send(
        endpoint: String,
        p256dh: ByteArray,
        auth: ByteArray,
        payload: ByteArray,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null,
    ): Boolean {
        val body = webPush.getBody(payload, p256dh, auth)
        val headers = webPush.getHeaders(endpoint, ttl, topic, urgency)

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .uri(URI.create(endpoint))
            .apply { headers.forEach { setHeader(it.key, it.value) } }
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())

        return when (val statusCode = response.statusCode()) {
            200, 201, 202 -> true
            404, 410 -> false
            else -> throw RuntimeException("Unexpected status code $statusCode: ${response.body()}")
        }
    }
}
