package com.tellusr.searchindex.util

import kotlin.math.min

fun String.shortenTo(maxLength: Int?): String {
    val len = min(maxLength ?: length, length)
    return substring(0, len)
}
