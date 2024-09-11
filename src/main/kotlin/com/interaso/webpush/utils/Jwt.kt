package com.interaso.webpush.utils

import dev.whyoleg.cryptography.algorithms.*
import kotlin.math.*

internal suspend fun createJwt(subject: String, audience: String, expiration: Int, privateKey: ECDSA.PrivateKey): String {
    val expiresAt = System.currentTimeMillis() / 1000 + expiration
    val payload = """{"sub":"$subject","aud":"$audience","exp":$expiresAt}"""

    val encodedHeader = encodeBase64("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
    val encodedPayload = encodeBase64(payload.toByteArray())
    val encodedToken = "$encodedHeader.$encodedPayload"

    val signature = privateKey
        .signatureGenerator(SHA256, ECDSA.SignatureFormat.DER)
        .generateSignature(encodedToken.toByteArray())

    val encodedSignature = encodeBase64(convertDerToJose(signature))

    return "$encodedToken.$encodedSignature"
}

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
