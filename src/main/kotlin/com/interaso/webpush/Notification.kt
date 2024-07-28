package com.interaso.webpush

import kotlin.time.*
import kotlin.time.Duration.Companion.days

public class Notification(
    public val payload: ByteArray,
    public val ttl: Duration = DEFAULT_TTL,
    public val topic: String? = null,
    public val urgency: Urgency? = null,
) {
    public constructor(payload: String, ttl: Duration = DEFAULT_TTL, topic: String? = null, urgency: Urgency? = null) : this(payload.encodeToByteArray(), ttl, topic, urgency)

    public companion object {
        public val DEFAULT_TTL: Duration = 28.days
    }
}
