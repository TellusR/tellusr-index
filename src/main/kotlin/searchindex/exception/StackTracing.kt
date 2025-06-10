package com.tellusr.searchindex.exception

import com.tellusr.searchindex.util.AppInfo
import java.io.PrintWriter
import java.io.StringWriter


val Throwable.stackTraceString: String
    get() {
        val stringWriter = StringWriter()
        this.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
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
