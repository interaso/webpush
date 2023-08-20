package com.interaso.webpush

import java.net.*
import java.security.interfaces.*

/**
 * Represents a web push notification request builder.
 *
 * @property subject the subject of the push service
 * @property vapidKeys the VapidKeys used for authentication
 */
public class WebPush(
    public val subject: String,
    public val vapidKeys: VapidKeys,
) {
    init {
        require(subject.startsWith("mailto:") || subject.startsWith("https://")) {
            "Subject must start with 'mailto:' or 'https://'"
        }
    }

    public companion object {
        private const val DEFAULT_TTL = 28 * 24 * 60 * 60

        private val webPushInfo = "WebPush: info\u0000".toByteArray()
        private val keyInfo = "Content-Encoding: aes128gcm\u0000".toByteArray()
        private val nonceInfo = "Content-Encoding: nonce\u0000".toByteArray()
    }

    /**
     * Generates the body of a web push message.
     *
     * @param payload The payload to be encrypted.
     * @param p256dh The public key of the user.
     * @param auth The authentication secret.
     * @return The encrypted body of the web push message.
     */
    public fun getBody(payload: ByteArray, p256dh: ByteArray, auth: ByteArray): ByteArray {
        val userPublicKey = generatePublicKeyFromUncompressedBytes(p256dh)
        val auxKeyPair = generateSecp256r1KeyPair()
        val auxPublicKey = getUncompressedBytes(auxKeyPair.public as ECPublicKey)
        val secret = generateEcdhSharedSecret(auxKeyPair.private as ECPrivateKey, userPublicKey)
        val salt = generateSalt(16)
        val secretInfo = concatBytes(webPushInfo, p256dh, auxPublicKey)
        val derivedSecret = hkdfSha256(secret, auth, secretInfo, 32)
        val derivedKey = hkdfSha256(derivedSecret, salt, keyInfo, 16)
        val derivedNonce = hkdfSha256(derivedSecret, salt, nonceInfo, 12)
        val encryptedPayload = encryptAesGcmNoPadding(derivedKey, derivedNonce, payload + byteArrayOf(2))

        return concatBytes(
            salt,
            byteArrayOf(0, 0, 16, 0),
            byteArrayOf(auxPublicKey.size.toByte()),
            auxPublicKey,
            encryptedPayload,
        )
    }

    /**
     * Retrieves the headers for a given push notification configuration.
     *
     * @param endpoint The endpoint URL.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     * @return A map containing the request headers.
     */
    public fun getHeaders(endpoint: String, ttl: Int?, topic: String?, urgency: Urgency?): Map<String, String> {
        return getHeadersWithToken(getToken(getAudience(endpoint)), ttl, topic, urgency)
    }

    /**
     * Returns the JWT audience from the specified endpoint.
     *
     * @param endpoint The endpoint URL.
     * @return The audience extracted from the endpoint URL.
     */
    public fun getAudience(endpoint: String): String {
        return URI(endpoint).run { "$scheme://$authority" }
    }

    /**
     * Returns a JSON Web Token (JWT) for VAPID authentication.
     *
     * @param audience The audience for which the JWT is intended. You can generate one using [getAudience] function.
     * @param expiration The expiration time of the JWT in seconds. Default value is 12 hours.
     * @return The generated JWT as a string.
     */
    public fun getToken(audience: String, expiration: Int = 12 * 60 * 60): String {
        return createEs256Jwt(subject, audience, expiration, vapidKeys.privateKey)
    }

    /**
     * Retrieves the headers with custom JWT token for a given push notification configuration.
     *
     * @param token The JWT token to include in the headers.
     * @param ttl The time-to-live value for the push notification (optional).
     * @param topic The topic of the push notification (optional).
     * @param urgency The urgency level of the push notification (optional).
     * @return A map containing the request headers.
     */
    public fun getHeadersWithToken(token: String, ttl: Int?, topic: String?, urgency: Urgency?): Map<String, String> {
        return buildMap(6) {
            put("Authorization", "vapid t=$token, k=${encodeBase64(vapidKeys.applicationServerKey)}")
            put("Content-Encoding", "aes128gcm")
            put("Content-Type", "application/octet-stream")
            put("TTL", (ttl ?: DEFAULT_TTL).toString())
            urgency?.let {
                put("Urgency", urgency.headerValue)
            }
            topic?.let {
                put("Topic", topic)
            }
        }
    }

    /**
     * Represents the urgency level of push notification.
     *
     * @property headerValue The header value associated with the urgency level.
     */
    public enum class Urgency(internal val headerValue: String) {
        VeryLow("very-low"),
        Low("low"),
        Normal("normal"),
        High("high"),
    }
}
