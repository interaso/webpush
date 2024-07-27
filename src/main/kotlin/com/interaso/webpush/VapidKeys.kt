package com.interaso.webpush

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.asymmetric.*

/**
 * Represents VapidKeys used for web push notifications.
 *
 * @property publicKey The public key of the VapidKeys.
 * @property privateKey The private key of the VapidKeys.
 * @property applicationServerKey The uncompressed bytes of the public key.
 *
 * @constructor Creates a VapidKeys instance with the specified public and private keys.
 *
 * @throws IllegalArgumentException If the provided public key does not correspond to the private key.
 */
public class VapidKeys(
    public val publicKey: ECDSA.PublicKey,
    public val privateKey: ECDSA.PrivateKey,
) {
    public val applicationServerKey: ByteArray = publicKey.encodeToBlocking(EC.PublicKey.Format.RAW)

    public companion object Factory {
        /**
         * Generates VapidKeys using the secp256r1 elliptic curve algorithm.
         *
         * @return the generated VapidKeys object.
         */
        @JvmStatic
        public fun generate(): VapidKeys {
            return CryptographyProvider.Default
                .get(ECDSA)
                .keyPairGenerator(EC.Curve.P256)
                .generateKeyBlocking()
                .let { VapidKeys(it.publicKey, it.privateKey) }
        }

        /**
         * Decodes the specified public key and private key from uncompressed bytes format and returns the corresponding VapidKeys object.
         *
         * @param publicKey The uncompressed public key bytes encoded as a Base64 string.
         * @param privateKey The uncompressed private key bytes encoded as a Base64 string.
         * @return The VapidKeys object representing the loaded public and private keys.
         */
        @JvmStatic
        public fun fromUncompressedBytes(publicKey: String, privateKey: String): VapidKeys {
            val decodedPublicKey = CryptographyProvider.Default
                .get(ECDSA)
                .publicKeyDecoder(EC.Curve.P256)
                .decodeFromBlocking(EC.PublicKey.Format.RAW, decodeBase64(publicKey))

            // TODO: EC.PrivateKey.Format.RAW: not implemented yet

            val decodedPrivateKey = CryptographyProvider.Default
                .get(ECDSA)
                .privateKeyDecoder(EC.Curve.P256)
                .decodeFromBlocking(EC.PrivateKey.Format.DER, decodeBase64(privateKey))

            return VapidKeys(decodedPublicKey, decodedPrivateKey)
        }
    }
}
