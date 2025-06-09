package com.tellusr.searchindex.tool

import com.tellusr.searchindex.exception.TellusRIndexException
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import kotlinx.serialization.json.JsonObject
import org.apache.lucene.document.Document

class SiCombinedSchema(first: SiSchema, second: SiSchema) : SiSchema(
    "${first.group}.${second.group}",
    listOf(
        first.fields,
        second.fields.map {
            SiSecondaryFieldWrapper(it)
        }
    ).flatten()
) {
    override val basePath: String? = null

    override fun fromDoc(doc: Document): SiRecord {
        throw TellusRIndexException.ShouldNotHappen("Not applicable for ${this::class.simpleName}")
    }


    override fun fromJson(jsonObject: JsonObject): SiRecord {
        throw TellusRIndexException.ShouldNotHappen("Not applicable for ${this::class.simpleName}")
    }
}
