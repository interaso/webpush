package com.interaso.webpush.utils

import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
internal val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
