package com.tellusr.searchindex.util

import org.slf4j.LoggerFactory


fun Any.getAutoNamedLogger() = if (javaClass.enclosingClass != null && javaClass.simpleName.contains("Companion")) {
    javaClass.declaringClass
} else {
    javaClass
}.let {
    LoggerFactory.getLogger(it)
}
