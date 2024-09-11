package com.interaso.webpush.utils

import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

@OptIn(ExperimentalEncodingApi::class)
internal fun encodeBase64(bytes: ByteArray): String {
    return base64.encode(bytes)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(string: String): ByteArray {
    return base64.decode(string)
}
