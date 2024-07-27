package com.interaso.webpush

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.random.*
import java.net.*

/**
 * Represents a web push notification request builder.
 *
 * @property subject The subject identifying the push notification sender. It must start with "mailto:" or "https://".
 * @property vapidKeys The VapidKeys object containing the public and private keys for VAPID authentication.
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

    private companion object {
        private const val DEFAULT_TTL = 28 * 24 * 60 * 60 // 28 days

        private val webPushInfo = "WebPush: info\u0000".toByteArray()
        private val keyInfo = "Content-Encoding: aes128gcm\u0000".toByteArray()
        private val nonceInfo = "Content-Encoding: nonce\u0000".toByteArray()
    }

    /**
     * Generates the body of request to push service provider
     *
     * @param payload The message payload to be sent in the push notification.
     * @param p256dh The Base64-encoded P256DH key for authentication with the push service provider.
     * @param auth The Base64-encoded authentication secret for the push service provider.
     * @return The encrypted body of the web push message.
     */
    public fun getBody(payload: ByteArray, p256dh: ByteArray, auth: ByteArray): ByteArray {
        val userPublicKey = CryptographyProvider.Default
            .get(ECDH)
            .publicKeyDecoder(EC.Curve.P256)
            .decodeFromBlocking(EC.PublicKey.Format.RAW, p256dh)

        val auxKeyPair = CryptographyProvider.Default
            .get(ECDH)
            .keyPairGenerator(EC.Curve.P256)
            .generateKeyBlocking()

        val auxPublicKey = auxKeyPair.publicKey
            .encodeToBlocking(EC.PublicKey.Format.RAW)

        val secret = auxKeyPair.privateKey
            .sharedSecretDerivation()
            .deriveSharedSecretBlocking(userPublicKey)

        val salt = CryptographyRandom.nextBytes(16)
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
     * @param endpoint The URL endpoint that identifies the push service subscription.
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
     * @param endpoint The URL endpoint that identifies the push service subscription.
     * @return The audience extracted from the endpoint URL.
     */
    public fun getAudience(endpoint: String): String {
        return URI(endpoint).run { "$scheme://$authority" }
    }

    /**
     * Returns a JSON Web Token (JWT) for VAPID authentication.
     *
     * @param audience The audience for which the JWT is intended. You can generate one using [getAudience] function.
     * @param expiration The expiration time of the JWT in seconds. The default value is 12 hours.
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
     * Returns the subscription state based on the provided status code.
     *
     * @param statusCode the status code received from the server
     * @param body the response body received from the server (optional)
     * @return the subscription state based on the provided status code
     * @throws WebPushStatusException if authentication failed (status code 401 or 403),
     *                                if the service is unavailable (status code 502 or 503),
     *                                or if an unexpected response is received
     */
    public fun getSubscriptionState(statusCode: Int, body: String? = null): SubscriptionState {
        return when (statusCode) {
            200, 201, 202 -> SubscriptionState.ACTIVE
            404, 410 -> SubscriptionState.EXPIRED
            401, 403 -> throw WebPushStatusException(statusCode, "Authentication failed: [$statusCode] - $body")
            502, 503 -> throw WebPushStatusException(statusCode, "Service unavailable: [$statusCode] - $body")
            else -> throw WebPushStatusException(statusCode, "Unexpected response: [$statusCode] - $body")
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

    /**
     * Represents the possible states of a push subscription.
     */
    public enum class SubscriptionState {
        ACTIVE,
        EXPIRED,
    }
}
