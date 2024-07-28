package com.interaso.webpush

public enum class Urgency(internal val headerValue: String) {
    VeryLow("very-low"),
    Low("low"),
    Normal("normal"),
    High("high"),
}
