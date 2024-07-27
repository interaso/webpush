package com.interaso.webpush

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.algorithms.asymmetric.*
import dev.whyoleg.cryptography.algorithms.digest.*
import dev.whyoleg.cryptography.algorithms.symmetric.*
import javax.crypto.*
import javax.crypto.spec.*
import kotlin.io.encoding.*
import kotlin.math.*

/**
 * Encrypts the given payload using AES-GCM with no padding.
 *
 * @param key The encryption key.
 * @param nonce The nonce.
 * @param payload The payload to be encrypted.
 * @return The encrypted payload.
 */
internal fun encryptAesGcmNoPadding(key: ByteArray, nonce: ByteArray, payload: ByteArray): ByteArray {
    // TODO: implement using CryptographyProvider

    //return CryptographyProvider.Default
    //    .get(AES.GCM)
    //    .keyDecoder()
    //    .decodeFromBlocking(AES.Key.Format.RAW, key)
    //    .cipher(128.bits)
    //    .encryptBlocking(nonce, payload)

    return Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        doFinal(payload)
    }
}

/**
 * Derives a key using HKDF algorithm with SHA-256 hash function.
 *
 * @param input The input key material.
 * @param salt The optional salt value, if not provided, a zero-length array is used.
 * @param info The optional context or application-specific information, if not provided, a zero-length array is used.
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
    return CryptographyProvider.Default
        .get(HMAC)
        .keyDecoder(SHA256)
        .decodeFromBlocking(HMAC.Key.Format.RAW, key)
        .signatureGenerator()
        .generateSignatureBlocking(data)
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
internal fun createEs256Jwt(subject: String, audience: String, expiration: Int, privateKey: ECDSA.PrivateKey): String {
    val expiresAt = System.currentTimeMillis() / 1000 + expiration
    val payload = """{"sub":"$subject","aud":"$audience","exp":$expiresAt}"""

    val encodedHeader = encodeBase64("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
    val encodedPayload = encodeBase64(payload.toByteArray())
    val encodedToken = "$encodedHeader.$encodedPayload"

    val signature = privateKey
        .signatureGenerator(SHA256, ECDSA.SignatureFormat.DER)
        .generateSignatureBlocking(encodedToken.toByteArray())

    val encodedSignature = encodeBase64(convertDerToJose(signature))

    return "$encodedToken.$encodedSignature"
}

/**
 * Encodes the given byte array to a Base64 string without padding.
 *
 * @param bytes The byte array to encode.
 * @return The Base64 encoded string.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun encodeBase64(bytes: ByteArray): String {
    return Base64.UrlSafe.encode(bytes).trimEnd('=')
}

/**
 * Decodes a Base64 encoded string into a byte array.
 *
 * @param string The Base64 encoded string to decode.
 * @return The decoded byte array.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(string: String): ByteArray {
    return Base64.UrlSafe.decode(string)
}

/**
 * Converts a DER-encoded signature to a JOSE-encoded signature.
 *
 * @param der The DER-encoded signature to convert.
 * @return The JOSE-encoded signature.
 */
private fun convertDerToJose(der: ByteArray): ByteArray {
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
