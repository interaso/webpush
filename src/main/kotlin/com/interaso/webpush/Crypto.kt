package com.interaso.webpush

import java.math.*
import java.security.*
import java.security.interfaces.*
import java.security.spec.*
import java.util.*
import javax.crypto.*
import javax.crypto.spec.*
import kotlin.math.*

/**
 * Standard name for the secp256r1 elliptic curve.
 */
private const val CURVE = "secp256r1"

/**
 * The ECParameterSpec for the secp256r1 elliptic curve.
 *
 * This variable represents the parameters for the secp256r1 elliptic curve,
 * which is also known as the P-256 curve. It is commonly used in cryptographic
 * algorithms such as the Elliptic Curve Digital Signature Algorithm (ECDSA).
 */
private val secp256r1parameterSpec: ECParameterSpec =
    AlgorithmParameters.getInstance("EC").run {
        init(ECGenParameterSpec(CURVE))
        getParameterSpec(ECParameterSpec::class.java)
    }

/**
 * Generates a key pair using the secp256r1 elliptic curve algorithm.
 *
 * @return A key pair containing the generated public and private keys.
 */
internal fun generateSecp256r1KeyPair(): KeyPair {
    return KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(CURVE))
        generateKeyPair()
    }
}

/**
 * Checks if the provided EC private key and public key form a valid key pair.
 *
 * @param privateKey The EC private key to be tested.
 * @param publicKey The EC public key to be tested.
 * @return `true` if the provided EC private key and public key form a valid key pair, `false` otherwise.
 */
internal fun areKeysValidPair(privateKey: ECPrivateKey, publicKey: ECPublicKey): Boolean {
    return Signature.getInstance("SHA256withECDSA").run {
        val test = byteArrayOf(1, 2, 3)
        initSign(privateKey)
        update(test)
        val signature = sign()
        initVerify(publicKey)
        update(test)
        verify(signature)
    }
}

/**
 * Generates an EC private key from a PKCS8 encoded byte array.
 *
 * @param bytes The PKCS8 encoded byte array.
 * @return The EC private key.
 */
internal fun generatePrivateKeyFromPkcs8(bytes: ByteArray): ECPrivateKey {
    return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(bytes)) as ECPrivateKey
}

/**
 * Generates a public key from the provided X.509 encoded byte array.
 *
 * @param bytes The X.509 encoded byte array.
 * @return The EC public key generated from the provided byte array.
 */
internal fun generatePublicKeyFromX509(bytes: ByteArray): ECPublicKey {
    return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes)) as ECPublicKey
}

/**
 * Encrypts the given payload using AES-GCM with no padding.
 *
 * @param key The encryption key.
 * @param nonce The nonce.
 * @param payload The payload to be encrypted.
 * @return The encrypted payload.
 */
internal fun encryptAesGcmNoPadding(key: ByteArray, nonce: ByteArray, payload: ByteArray): ByteArray {
    return Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        doFinal(payload)
    }
}

/**
 * Generates an ECDH shared secret between the given private key and public key.
 *
 * @param privateKey The ECPrivateKey to be used for generating the shared secret.
 * @param publicKey The ECPublicKey to be used for generating the shared secret.
 * @return The generated shared secret as a byte array.
 */
internal fun generateEcdhSharedSecret(privateKey: ECPrivateKey, publicKey: ECPublicKey): ByteArray {
    return KeyAgreement.getInstance("ECDH").run {
        init(privateKey)
        doPhase(publicKey, true)
        generateSecret()
    }
}

/**
 * Derives a key using HKDF algorithm with SHA-256 hash function.
 *
 * @param input The input key material.
 * @param salt The optional salt value, if not provided, a zero-length array is used.
 * @param info The optional context or application specific information, if not provided, a zero-length array is used.
 * @param length The desired length of the derived key.
 * @return The derived key as a byte array.
 */
internal fun hkdfSha256(input: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    return hmacSha256(hmacSha256(salt, input), info + 0x01.toByte()).copyOfRange(0, length)
}

/**
 * Computes the HMAC-SHA256 hash of the given data using the provided key.
 *
 * @param key The key to use for HMAC-SHA256 computation.
 * @param data The data to compute the HMAC-SHA256 hash for.
 * @return The HMAC-SHA256 hash of the given data as a byte array.
 */
private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    return Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }
}

/**
 * Creates a JSON Web Token (JWT) using the ES256 algorithm.
 *
 * @param subject the subject of the token
 * @param audience the audience of the token
 * @param expiration the expiration time (in seconds) of the token
 * @param privateKey the private key used to sign the token
 * @return the generated JWT
 */
internal fun createEs256Jwt(subject: String, audience: String, expiration: Int, privateKey: ECPrivateKey): String {
    val expiresAt = System.currentTimeMillis() / 1000 + expiration
    val payload = """{"sub":"$subject","aud":"$audience","exp":$expiresAt}"""

    val encodedHeader = encodeBase64("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
    val encodedPayload = encodeBase64(payload.toByteArray())
    val encodedToken = "$encodedHeader.$encodedPayload"

    val signature = signSha256withEcdsa(privateKey, encodedToken.toByteArray())
    val encodedSignature = encodeBase64(convertDerToJose(signature))

    return "$encodedToken.$encodedSignature"
}

/**
 * Signs the given byte array using SHA256 with ECDSA algorithm.
 *
 * @param privateKey the ECPrivateKey used for signing
 * @param bytes the byte array to be signed
 * @return the signature as byte array
 */
private fun signSha256withEcdsa(privateKey: ECPrivateKey, bytes: ByteArray): ByteArray {
    return Signature.getInstance("SHA256withECDSA").run {
        initSign(privateKey)
        update(bytes)
        sign()
    }
}

/**
 * Converts the specified public key to uncompressed bytes format.
 *
 * @param publicKey the EC public key to be converted
 * @return the uncompressed bytes representation of the public key
 */
internal fun getUncompressedBytes(publicKey: ECPublicKey): ByteArray {
    val x = publicKey.w.affineX.toByteArray()
    val y = publicKey.w.affineY.toByteArray()

    val xStart = if (x.size == 33 && x[0].toInt() == 0) 1 else 0
    val yStart = if (y.size == 33 && y[0].toInt() == 0) 1 else 0

    return ByteArray(65).also { array ->
        array[0] = 0x04
        x.copyInto(array, 1 + 32 - (x.size - xStart), xStart, x.size)
        y.copyInto(array, 1 + 64 - (y.size - yStart), yStart, y.size)
    }
}

/**
 * Generate an EC public key from uncompressed bytes.
 *
 * @param bytes The uncompressed bytes representing the public key.
 * @return The generated EC public key.
 */
internal fun generatePublicKeyFromUncompressedBytes(bytes: ByteArray): ECPublicKey {
    val ecPoint = ECPoint(
        BigInteger(1, bytes.copyOfRange(1, 33)),
        BigInteger(1, bytes.copyOfRange(33, 65)),
    )

    return KeyFactory.getInstance("EC").run {
        generatePublic(ECPublicKeySpec(ecPoint, secp256r1parameterSpec)) as ECPublicKey
    }
}

/**
 * Generate an EC private key from uncompressed bytes.
 *
 * @param bytes The uncompressed bytes representing the private key.
 * @return The generated EC private key.
 */
internal fun generatePrivateKeyFromUncompressedBytes(bytes: ByteArray): ECPrivateKey {
    return KeyFactory.getInstance("EC").run {
        generatePrivate(ECPrivateKeySpec(BigInteger(bytes), secp256r1parameterSpec)) as ECPrivateKey
    }
}

/**
 * Generates a salt of the specified size.
 *
 * @param size The size of the salt to generate.
 * @return The generated salt as a byte array.
 */
internal fun generateSalt(size: Int): ByteArray {
    return SecureRandom.getSeed(size)
}

/**
 * Encodes the given byte array to a Base64 string without padding.
 *
 * @param bytes The byte array to encode.
 * @return The Base64 encoded string.
 */
internal fun encodeBase64(bytes: ByteArray): String {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

/**
 * Decodes a Base64 encoded string into a byte array.
 *
 * @param string The Base64 encoded string to decode.
 * @return The decoded byte array.
 */
internal fun decodeBase64(string: String): ByteArray {
    return Base64.getUrlDecoder().decode(string)
}

/**
 * Converts a DER-encoded signature to a JOSE-encoded signature.
 *
 * @param der The DER-encoded signature to convert.
 * @return The JOSE-encoded signature.
 */
internal fun convertDerToJose(der: ByteArray): ByteArray {
    val numberSize = 32
    var offset = 3
    val jose = ByteArray(numberSize * 2)

    if (der[1] == 0x81.toByte()) {
        offset++
    }

    val rLength = der[offset++].toInt()
    val rPadding = numberSize - rLength

    der.copyInto(
        destination = jose,
        destinationOffset = max(rPadding, 0),
        startIndex = offset + max(-rPadding, 0),
        endIndex = offset + max(-rPadding, 0) + (rLength - max(-rPadding, 0))
    )

    offset += rLength + 1

    val sLength = der[offset++].toInt()
    val sPadding = numberSize - sLength

    der.copyInto(
        destination = jose,
        destinationOffset = numberSize + max(sPadding, 0),
        startIndex = offset + max(-sPadding, 0),
        endIndex = offset + max(-sPadding, 0) + (sLength - max(-sPadding, 0))
    )

    return jose
}

/**
 * Concatenates multiple byte arrays into a single byte array.
 *
 * @param arrays the byte arrays to be concatenated
 * @return the concatenated byte array
 */
internal fun concatBytes(vararg arrays: ByteArray): ByteArray {
    val result = ByteArray(arrays.sumOf { it.size })
    var position = 0

    for (array in arrays) {
        array.copyInto(result, position)
        position += array.size
    }

    return result
}
