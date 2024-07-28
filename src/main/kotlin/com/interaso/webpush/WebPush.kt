package com.interaso.webpush

import com.interaso.webpush.utils.*
import com.interaso.webpush.vapid.*
import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.algorithms.symmetric.*
import dev.whyoleg.cryptography.random.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.*

public class WebPush(
    private val subject: String,
    private val vapidKeysProvider: VapidKeysProvider,
    private val httpClient: HttpClient = HttpClient(),
) {
    private var vapidKeys: VapidKeys? = null
    private val mutex = Mutex()

    init {
        require(subject.startsWith("mailto:") || subject.startsWith("https://")) {
            "Subject must start with 'mailto:' or 'https://'"
        }
    }

    public constructor(subject: String, vapidKeys: VapidKeys) : this(subject, DefaultVapidKeysProvider(vapidKeys))
    public constructor(subject: String, publicKey: String, privateKey: String) : this(subject, StringVapidKeysProvider(publicKey, privateKey))

    public companion object {
        private val webPushInfo = "WebPush: info\u0000".toByteArray()
        private val keyInfo = "Content-Encoding: aes128gcm\u0000".toByteArray()
        private val nonceInfo = "Content-Encoding: nonce\u0000".toByteArray()

        public var cryptographyProvider: CryptographyProvider = CryptographyProvider.Default
    }

    private suspend fun getVapidKeys(): VapidKeys {
        return vapidKeys ?: mutex.withLock {
            vapidKeys ?: vapidKeysProvider.get().also { vapidKeys = it }
        }
    }

    public suspend fun getApplicationServerKey(): ByteArray {
        return getVapidKeys().publicKey.encodeTo(EC.PublicKey.Format.RAW)
    }

    public suspend fun send(subscription: Subscription, notification: Notification): SubscriptionState {
        val token = createJwt(
            subject = subject,
            audience = Url(subscription.endpoint).protocolWithAuthority,
            expiration = 12 * 60 * 60,
            privateKey = getVapidKeys().privateKey,
        )

        val key = encodeBase64(getApplicationServerKey())
        val body = encryptBody(notification.payload, subscription.p256dh, subscription.auth)

        val response = httpClient.post(subscription.endpoint) {
            header("Authorization", "vapid t=$token, k=$key")
            header("Content-Encoding", "aes128gcm")
            header("Content-Type", "application/octet-stream")
            header("TTL", notification.ttl)
            header("Urgency", notification.urgency?.headerValue)
            header("Topic", notification.topic)
            setBody(body)
        }

        val responseText = response.bodyAsText().take(200)

        return when (response.status.value) {
            200, 201, 202 -> SubscriptionState.ACTIVE
            404, 410 -> SubscriptionState.EXPIRED
            401, 403 -> throw WebPushException("Authentication failed: $responseText")
            502, 503 -> throw WebPushException("Service unavailable: $responseText")
            else -> throw WebPushException("Unexpected response: $responseText")
        }
    }

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun encryptBody(payload: ByteArray, p256dh: ByteArray, auth: ByteArray): ByteArray {
        val userPublicKey = cryptographyProvider
            .get(ECDH)
            .publicKeyDecoder(EC.Curve.P256)
            .decodeFrom(EC.PublicKey.Format.RAW, p256dh)

        val auxKeyPair = cryptographyProvider
            .get(ECDH)
            .keyPairGenerator(EC.Curve.P256)
            .generateKey()

        val auxPublicKey = auxKeyPair.publicKey
            .encodeTo(EC.PublicKey.Format.RAW)

        val secret = auxKeyPair.privateKey
            .sharedSecretDerivation()
            .deriveSharedSecret(userPublicKey)

        val salt = CryptographyRandom.nextBytes(16)
        val secretInfo = concatBytes(webPushInfo, p256dh, auxPublicKey)
        val derivedSecret = hkdfSha256(secret, auth, secretInfo, 32)
        val derivedKey = hkdfSha256(derivedSecret, salt, keyInfo, 16)
        val derivedNonce = hkdfSha256(derivedSecret, salt, nonceInfo, 12)

        val encryptedPayload = cryptographyProvider
            .get(AES.GCM)
            .keyDecoder()
            .decodeFrom(AES.Key.Format.RAW, derivedKey)
            .cipher()
            .encrypt(derivedNonce, payload + byteArrayOf(2), null)

        return concatBytes(
            salt,
            byteArrayOf(0, 0, 16, 0),
            byteArrayOf(auxPublicKey.size.toByte()),
            auxPublicKey,
            encryptedPayload,
        )
    }
}
