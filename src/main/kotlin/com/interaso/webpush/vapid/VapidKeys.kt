package com.interaso.webpush.vapid

import com.interaso.webpush.*
import com.interaso.webpush.utils.encodeBase64
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.serialization.asn1.*
import dev.whyoleg.cryptography.serialization.asn1.modules.*
import kotlinx.serialization.*

public data class VapidKeys(
    public val publicKey: ECDSA.PublicKey,
    public val privateKey: ECDSA.PrivateKey,
) {
    public companion object {
        public suspend fun generate(): VapidKeys {
            return WebPush.cryptographyProvider
                .get(ECDSA)
                .keyPairGenerator(EC.Curve.P256)
                .generateKey()
                .let { VapidKeys(it.publicKey, it.privateKey) }
        }
    }
}

public suspend fun VapidKeys.exportPublicKey(): String {
    return encodeBase64(publicKey.encodeTo(EC.PublicKey.Format.RAW))
}

public suspend fun VapidKeys.exportPrivateKey(): String {
    return privateKey
        .encodeTo(EC.PrivateKey.Format.DER.SEC1)
        .let { DER.decodeFromByteArray<EcPrivateKey>(it) }
        .privateKey
        .let { encodeBase64(it) }
}
