package com.interaso.webpush.vapid

import com.interaso.webpush.*
import com.interaso.webpush.utils.decodeBase64
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.serialization.asn1.*
import dev.whyoleg.cryptography.serialization.asn1.modules.*
import kotlinx.serialization.*

public interface VapidKeysProvider {
    public suspend fun get(): VapidKeys
}

public class DefaultVapidKeysProvider(private val vapidKeys: VapidKeys) : VapidKeysProvider {
    override suspend fun get(): VapidKeys {
        return vapidKeys
    }
}

public class StringVapidKeysProvider(
    private val publicKey: String,
    private val privateKey: String,
) : VapidKeysProvider {
    override suspend fun get(): VapidKeys {
        val decodedPublicKey = WebPush.cryptographyProvider
            .get(ECDSA)
            .publicKeyDecoder(EC.Curve.P256)
            .decodeFrom(EC.PublicKey.Format.RAW, decodeBase64(publicKey))

        val derPrivateKey = DER.encodeToByteArray(EcPrivateKey(1, decodeBase64(privateKey), EcParameters(ObjectIdentifier.secp256r1)))

        val decodedPrivateKey = WebPush.cryptographyProvider
            .get(ECDSA)
            .privateKeyDecoder(EC.Curve.P256)
            .decodeFrom(EC.PrivateKey.Format.DER.SEC1, derPrivateKey)

        return VapidKeys(decodedPublicKey, decodedPrivateKey)
    }
}
