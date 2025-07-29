package com.tellusr.searchindex

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import com.tellusr.searchindex.*
import org.apache.lucene.document.Document

// Used for testing and demo purposes only.
interface TestStore {
    enum class Fields : SiField {
        Id, Instruction, Context, Response, Category, Categories, Language, SourceId, MetaData, Vectorized;


        override fun primary(): String = name
        override fun fieldType(): Set<SiFieldType> = when (this) {
            Id -> setOf(SiFieldType.UniqueId)
            Category -> setOf(SiFieldType.Keyword)
            Categories -> setOf(SiFieldType.Keyword)
            SourceId -> setOf(SiFieldType.ForeignKey)
            Vectorized -> setOf(SiFieldType.Vector)
            else -> setOf(SiFieldType.Auto)
        }
    }


    object Schema : SiSchema("unit-test", Fields.entries) {
        override fun fromDoc(doc: Document): SiRecord = Record(doc)
        override fun fromJson(jsonObject: JsonObject): Record = json.decodeFromJsonElement<Record>(jsonObject)
    }


    @Serializable
    class Record(
        var id: String,
        var instruction: String,
        var context: String?,
        var response: String?,
        var category: String?,
        var language: String?,
        var sourceId: String? = null,
        var json: JsonElement? = null,
        var vectorized: FloatArray? = null,
        var categories: MutableList<String> = mutableListOf(),
    ) : SiRecord {
        constructor(id: String): this(
            id,
            "instruction $id",
            "context $id",
            "response $id",
            "a/$id",
            "language $id",
            "sourceId $id",
            null,
            null,
            mutableListOf("a/$id", "b/$id", "c/$id"),
            )

        constructor(doc: Document) : this(
            id = doc.get(Fields.Id.primary()),
            instruction = doc.get(Fields.Instruction.primary()),
            context = doc.get(Fields.Context.primary()),
            response = doc.get(Fields.Response.primary()),
            category = doc.get(Fields.Category.primary()),
            categories = doc.getValues(Fields.Categories.primary()).toMutableList(),
            language = doc.get(Fields.Language.primary()),
            sourceId = doc.get(Fields.SourceId.primary()),
            json = doc.get(Fields.MetaData.primary())?.let {
                SiRecord.jsonEncoder.parseToJsonElement(it)
            },
            vectorized = SiRecord.fromVectorString(doc.get(Fields.Vectorized.primary()))
        )

        override fun schema(): SiSchema = Schema

        override fun getValue(key: SiField): Any? = when (key as? Fields) {
            Fields.Id -> id
            Fields.Instruction -> instruction
            Fields.Context -> context
            Fields.Response -> response
            Fields.Category -> category
            Fields.Categories -> categories
            Fields.Language -> language
            Fields.SourceId -> sourceId
            Fields.MetaData -> json
            Fields.Vectorized -> vectorized
            null -> null
        }

        override fun setValue(key: SiField, value: Any?) {
            when (key as? Fields) {
                Fields.Id -> (value as? String)?.let { id = it }
                Fields.Instruction -> (value as? String)?.let { instruction = it }
                Fields.Context -> context = (value as? String)
                Fields.Response -> response = (value as? String)
                Fields.Category -> category = (value as? String)
                Fields.Categories -> categories = when (value) {
                    is String -> value.split(',').map { it.trim() }.toMutableList()
                    is List<*> -> value.mapNotNull { it as? String }.toMutableList()
                    else -> mutableListOf()
                }
                Fields.Language -> language = (value as? String)
                Fields.SourceId -> sourceId = (value as? String)
                Fields.MetaData -> json = when (value) {
                    is JsonElement -> value
                    is String -> SiRecord.jsonEncoder.parseToJsonElement(value)
                    else -> null
                }
                Fields.Vectorized -> vectorized = value as FloatArray
                null -> {}
            }
        }
    }

    class SearchIndex(name: String) : SiSearchIndex<Record>(Schema, name)
}
