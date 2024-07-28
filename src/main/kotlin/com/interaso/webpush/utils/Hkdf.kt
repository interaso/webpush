package com.interaso.webpush.utils

import com.interaso.webpush.*
import dev.whyoleg.cryptography.algorithms.digest.*
import dev.whyoleg.cryptography.algorithms.symmetric.*

internal suspend fun hkdfSha256(input: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    return hmacSha256(hmacSha256(salt, input), info + 0x01.toByte()).copyOfRange(0, length)
}

private suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    return WebPush.cryptographyProvider
        .get(HMAC)
        .keyDecoder(SHA256)
        .decodeFrom(HMAC.Key.Format.RAW, key)
        .signatureGenerator()
        .generateSignature(data)
}
