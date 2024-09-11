package com.interaso.webpush

import com.interaso.webpush.utils.*
import kotlin.io.encoding.*

public class Subscription(
    public val endpoint: String,
    public val p256dh: ByteArray,
    public val auth: ByteArray,
) {
    @OptIn(ExperimentalEncodingApi::class)
    public constructor(endpoint: String, p256dh: String, auth: String) : this(endpoint, base64.decode(p256dh), base64.decode(auth))
}
