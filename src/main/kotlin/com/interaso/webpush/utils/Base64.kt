package com.interaso.webpush.utils

import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
internal fun encodeBase64(bytes: ByteArray): String {
    return Base64.UrlSafe.encode(bytes).trimEnd('=')
}

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(string: String): ByteArray {
    return Base64.UrlSafe.decode(string)
}
