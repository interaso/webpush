package com.interaso.webpush.utils

internal fun concatBytes(vararg arrays: ByteArray): ByteArray {
    val result = ByteArray(arrays.sumOf { it.size })
    var position = 0

    for (array in arrays) {
        array.copyInto(result, position)
        position += array.size
    }

    return result
}
