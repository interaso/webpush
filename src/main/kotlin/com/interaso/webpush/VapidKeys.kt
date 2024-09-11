package com.interaso.webpush

import com.interaso.webpush.utils.*
import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.serialization.asn1.*
import dev.whyoleg.cryptography.serialization.asn1.modules.*
import kotlinx.serialization.*
import kotlin.io.encoding.*

public data class VapidKeys(
    public val publicKey: ECDSA.PublicKey,
    public val privateKey: ECDSA.PrivateKey,
) {
    public companion object Factory {
        public suspend fun generate(): VapidKeys {
            return CryptographyProvider.Default
                .get(ECDSA)
                .keyPairGenerator(EC.Curve.P256)
                .generateKey()
                .let { VapidKeys(it.publicKey, it.privateKey) }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
public suspend fun VapidKeys.Factory.fromBase64(publicKey: String, privateKey: String): VapidKeys {
    val decodedPublicKey = CryptographyProvider.Default
        .get(ECDSA)
        .publicKeyDecoder(EC.Curve.P256)
        .decodeFromByteArray(EC.PublicKey.Format.RAW, base64.decode(publicKey))

    val derPrivateKey = Der.encodeToByteArray(EcPrivateKey(1, base64.decode(privateKey), EcParameters(ObjectIdentifier.secp256r1)))

    val decodedPrivateKey = CryptographyProvider.Default
        .get(ECDSA)
        .privateKeyDecoder(EC.Curve.P256)
        .decodeFromByteArray(EC.PrivateKey.Format.DER.SEC1, derPrivateKey)

    return VapidKeys(decodedPublicKey, decodedPrivateKey)
}

@OptIn(ExperimentalEncodingApi::class)
public suspend fun VapidKeys.publicKeyAsBase64(): String {
    return base64.encode(publicKey.encodeToByteArray(EC.PublicKey.Format.RAW))
}

@OptIn(ExperimentalEncodingApi::class)
public suspend fun VapidKeys.privateKeyAsBase64(): String {
    return privateKey
        .encodeToByteArray(EC.PrivateKey.Format.DER.SEC1)
        .let { Der.decodeFromByteArray<EcPrivateKey>(it) }
        .privateKey
        .let { base64.encode(it) }
}
