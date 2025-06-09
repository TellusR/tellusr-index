package com.tellusr.searchindex.util

object AppInfo {
    val group: String = "com.tellusr.searchindex"
    val version: String = AppInfo::class.java.getPackage().implementationVersion ?: "(Unversioned)"
}
