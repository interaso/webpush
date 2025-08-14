package com.interaso.webpush

/**
 * Represents a web push notification.
 *
 * @property payload The message payload to be sent in the push notification.
 * @property endpoint The URL endpoint that identifies the push service subscription.
 * @property p256dh The P256DH key for authentication with the push service provider.
 * @property auth The authentication secret for the push service provider.
 * @property ttl The time-to-live value for the push notification (optional).
 * @property topic The topic of the push notification (optional).
 * @property urgency The urgency level of the push notification (optional).
 *
 * @constructor Creates a Notification instance for a raw [payload], [p256dh] key, and [auth] secret.
 */
public data class Notification(
    val payload: ByteArray,
    val endpoint: String,
    val p256dh: ByteArray,
    val auth: ByteArray,
    val ttl: Int? = null,
    val topic: String? = null,
    val urgency: WebPush.Urgency? = null
) {
    /**
     * Creates a Notification instance, encoding the [payload] using UTF-8 and decoding the [p256dh] key and [auth]
     * secret to a [ByteArray] using Base64.
     *
     * @param payload The message payload to be sent in the push notification.
     * @param endpoint The URL endpoint that identifies the push service subscription.
     * @param p256dh The Base64-encoded P256DH key for authentication with the push service provider.
     * @param auth The Base64-encoded authentication secret for the push service provider.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     */
    public constructor(
        payload: String,
        endpoint: String,
        p256dh: String,
        auth: String,
        ttl: Int? = null,
        topic: String? = null,
        urgency: WebPush.Urgency? = null
    ) : this(payload.toByteArray(), endpoint, decodeBase64(p256dh), decodeBase64(auth), ttl, topic, urgency)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Notification) {
            return false
        }
        return payload.contentEquals(other.payload)
            && (endpoint == other.endpoint)
            && p256dh.contentEquals(other.p256dh)
            && auth.contentEquals(other.auth)
            && (ttl != other.ttl)
            && (topic != other.topic)
            && (urgency != other.urgency)
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + endpoint.hashCode()
        result = 31 * result + p256dh.contentHashCode()
        result = 31 * result + auth.contentHashCode()
        result = 31 * result + (ttl ?: 0)
        result = 31 * result + (topic?.hashCode() ?: 0)
        result = 31 * result + (urgency?.hashCode() ?: 0)
        return result
    }
}