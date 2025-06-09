package com.tellusr.searchindex.exception

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import com.tellusr.searchindex.util.AppInfo
import java.io.PrintWriter
import java.io.StringWriter


fun Any.getLogableName() = if (this::class.isCompanion) {
    this::class.java.declaringClass.simpleName
} else {
    this::class.simpleName ?: "-"
}


val Throwable.stackTraceString: String
    get() {
        val stringWriter = StringWriter()
        this.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
    }


val Throwable.appStackTrace: String
    get() = listOf("${javaClass.simpleName}: ${localizedMessage ?: message}").plus(stackTrace.filter {
        it.className.startsWith(AppInfo.group)
    }).joinToString("\n  ")


val Throwable.jsonTellusrStackTrace: JsonArray
    get() = listOf("${javaClass.simpleName}: ${localizedMessage ?: message}").plus(stackTrace.filter {
        it.className.startsWith(AppInfo.group)
    }).map {
        JsonPrimitive(it.toString())
    }.let {
        JsonArray(it)
    }


val Throwable.stackTraceTopString: String?
    get() {
        var ex: Throwable? = this
        var res: String? = null
        while (res == null && ex != null) {
            res = ex.stackTrace.firstOrNull {
                it.className.startsWith(AppInfo.group)
            }?.toString()
            ex = ex.cause
        }
        return res
    }


val Throwable.messageAndCrumb: String
    get() {
        return "${javaClass.simpleName}: ${localizedMessage} (${stackTraceTopString ?: "-"})"
    }


inline val currentTellusrStackTrace: String get() {
    try {
        throw Exception("stacktrace")
    } catch (t: Throwable) {
        return t.appStackTrace
    }
}
