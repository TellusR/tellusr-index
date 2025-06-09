package com.tellusr.searchindex.util

import kotlinx.serialization.json.JsonElement

/**
 * Interface that makes objects parseable by ConvertUtil.
 * Implement this interface in your class to enable JSON serialization.
 */
interface JsonExporter {
    /**
     * Converts the implementing object to a JsonElement representation.
     * @return JsonElement containing the object's data
     */
    fun jsonExport(): JsonElement
}
