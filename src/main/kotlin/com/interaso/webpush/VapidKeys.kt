package com.interaso.webpush

import java.nio.file.*
import java.security.interfaces.*
import kotlin.io.path.*

/**
 * Represents VapidKeys used for web push notifications.
 *
 * @property publicKey The public key of the VapidKeys.
 * @property privateKey The private key of the VapidKeys.
 * @property applicationServerKey The uncompressed bytes of the public key.
 * @property x509PublicKey The base64 encoded string of the public key.
 * @property pkcs8PrivateKey The base64 encoded string of the private key.
 *
 * @constructor Creates a VapidKeys instance with the specified public and private keys.
 *
 * @throws IllegalArgumentException If the provided public key does not correspond to the private key.
 */
public class VapidKeys(
    public val publicKey: ECPublicKey,
    public val privateKey: ECPrivateKey,
) {
    public val applicationServerKey: ByteArray = getUncompressedBytes(publicKey)

    public val x509PublicKey: String = encodeBase64(publicKey.encoded)
    public val pkcs8PrivateKey: String = encodeBase64(privateKey.encoded)

    init {
        require(areKeysValidPair(privateKey, publicKey)) {
            "Public key does not corresponds to private key"
        }
    }

    public companion object Factory {
        /**
         * Generates VapidKeys using the secp256r1 elliptic curve algorithm.
         *
         * @return the generated VapidKeys object.
         */
        @JvmStatic
        public fun generate(): VapidKeys {
            return generateSecp256r1KeyPair().run {
                VapidKeys(public as ECPublicKey, private as ECPrivateKey)
            }
        }

        /**
         * Loads the specified X509 public key and PKCS8 private key and returns the corresponding VapidKeys object.
         *
         * @param x509PublicKey The X509 public key encoded as a Base64 string.
         * @param pkcs8PrivateKey The PKCS8 private key encoded as a Base64 string.
         * @return The VapidKeys object representing the loaded public and private keys.
         */
        @JvmStatic
        public fun load(x509PublicKey: String, pkcs8PrivateKey: String): VapidKeys {
            return VapidKeys(
                generatePublicKeyFromX509(decodeBase64(x509PublicKey)),
                generatePrivateKeyFromPkcs8(decodeBase64(pkcs8PrivateKey)),
            )
        }

        /**
         * Loads VapidKeys from a given path.
         *
         * @param path The path to load the VapidKeys from.
         * @param generateMissing If set to true and the path does not exist, generate new VapidKeys and save them to the path.
         *
         * @return The loaded VapidKeys object.
         */
        @JvmStatic
        public fun load(path: Path, generateMissing: Boolean = true): VapidKeys {
            if (!path.exists() && generateMissing) {
                return generate().apply {
                    path.createParentDirectories()
                    path.writeLines(listOf(x509PublicKey, pkcs8PrivateKey))
                }
            }

            return path.readLines().let { (x509PublicKey, pkcs8PrivateKey) ->
                load(x509PublicKey, pkcs8PrivateKey)
            }
        }
    }
}
