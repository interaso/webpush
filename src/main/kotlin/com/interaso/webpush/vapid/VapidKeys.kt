package com.interaso.webpush.vapid

import com.interaso.webpush.*
import com.interaso.webpush.utils.*
import dev.whyoleg.cryptography.algorithms.*
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
    return encodeBase64(publicKey.encodeToByteArray(EC.PublicKey.Format.RAW))
}

public suspend fun VapidKeys.exportPrivateKey(): String {
    return privateKey
        .encodeToByteArray(EC.PrivateKey.Format.DER.SEC1)
        .let { Der.decodeFromByteArray<EcPrivateKey>(it) }
        .privateKey
        .let { encodeBase64(it) }
}
