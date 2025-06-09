package com.tellusr.searchindex.tool

import com.tellusr.searchindex.exception.TellusRIndexException
import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.util.ConvertUtil
import com.tellusr.searchindex.util.getAutoNamedLogger
import kotlinx.serialization.json.JsonObject

class SiOneToOther<T1: SiRecord, T2: SiRecord>(val one: T1, val other: T2?) : SiRecord {
    override fun schema(): SiSchema = one.schema()


    override fun fields(): List<SiField> =
        listOfNotNull(
            one.fields(),
            other?.fields()?.map {
                SiSecondaryFieldWrapper(it)
            }
        ).flatten()


    override fun formFields(): List<SiField> =
        listOfNotNull(
            one.formFields(),
            other?.formFields()?.map {
                SiSecondaryFieldWrapper(it)
            }
        ).flatten()


    override fun getValue(key: SiField): Any? = if (one.fields().contains(key)) {
        one.getValue(key)
    } else {
        ((key as? SiSecondaryFieldWrapper)?.inner ?: key).let {
            other?.getValue(it)
        }
    }


    override fun setValue(key: SiField, value: Any?) =
        throw TellusRIndexException.SearchIndexError("Set value not supported for joined records")


    override fun toJson(includeFields: List<SiField>?, shortenTo: Int?): JsonObject =
        toMap(includeFields).entries.associate { (key, value) ->
            if (one.fields().contains(key)) {
                key.primary()
            } else {
                key.secondary()
            } to ConvertUtil.toJsonValue(value, shortenTo)
        }.let { r ->
            JsonObject(r.map { it.key to it.value }.toMap())
        }


    companion object {
        val logger = getAutoNamedLogger()
    }
}
