package com.interaso.webpush

import com.interaso.webpush.utils.*

public class Subscription(
    public val endpoint: String,
    public val p256dh: ByteArray,
    public val auth: ByteArray,
) {
    public constructor(endpoint: String, p256dh: String, auth: String) : this(endpoint, decodeBase64(p256dh), decodeBase64(auth))
}
