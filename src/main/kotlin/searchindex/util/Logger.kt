package com.tellusr.searchindex.util

import com.tellusr.searchindex.exception.getLogableName
import org.slf4j.LoggerFactory
import org.slf4j.LoggerFactory.getLogger
import java.time.Instant

fun Any.getLogableName() = if (this::class.isCompanion) {
    this::class.java.declaringClass.simpleName
} else {
    this::class.simpleName ?: this::class.java.name
}

fun Any.getAutoNamedLogger() = getLogger(getLogableName())


/**
 * A very handy extension function to retrieve a logger. Having all classes use this
 * centralized declaration also has the advantage that we are certain all instances using
 * this provider will follow the very same naming scheme.
 *
 * Also, we specify the return type as Logger (so not a nullable type). LoggerFactory.getLogger\
()
 * is a so-called «platform call», which makes it impossible for the kotlin compiler to be
 * certain about nullability, but we are willing to take the risk!
 */
fun getLogger(name: String): org.slf4j.Logger = LoggerFactory.getLogger(name)

fun getLogger(): org.slf4j.Logger = LoggerFactory.getLogger("-")

class LogEvery(private val logger: org.slf4j.Logger, private val seconds: Long = 60) {
    var nextLogInstant = Instant.EPOCH

    val log: org.slf4j.Logger?
        get() {
            if (nextLogInstant.isBefore(Instant.now())) {
                nextLogInstant = Instant.now().plusSeconds(seconds)
                return logger
            }
            return null
        }
}
