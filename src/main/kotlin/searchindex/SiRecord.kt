package com.tellusr.searchindex

import com.tellusr.searchindex.util.ConvertUtil
import kotlinx.serialization.json.*
import org.apache.lucene.document.Document


/**
 * Interface representing a search index record. Implements the core functionality for storing
 * and retrieving data in a search index. This interface provides methods for converting between
 * different data formats (Map, JSON, Lucene Document) and accessing field values.
 *
 * The interface is typically implemented to create specific record types that define how
 * data is stored and accessed in the search index. Each implementation must provide:
 * - A schema definition through [schema]
 * - Value access methods through [getValue] and [setValue]
 *
 * Key features:
 * - Conversion between different formats (Map, JSON, Lucene Document)
 * - Field value access and modification
 * - Schema-based field definitions
 * - Unique ID handling
 * - Logging support
 *
 * @see SiSchema
 * @see SiField
 */
interface SiRecord {
    /**
     * Returns the unique identifier for this record.
     * The unique ID is retrieved from the schema's defined ID field
     * and is used to uniquely identify this record in the search index.
     *
     * @return The unique identifier as a String, or null if no ID is set
     */
    fun uniqueId(): String? = schema().idField.let { getValue(it) as? String }

    /**
     * Defines the schema for the search index record. The schema contains
     * metadata and utility functions implementing a record factory and
     * other purposes.
     */
    fun schema(): SiSchema

    /**
     * Returns the list of fields defined by the schema of this record.
     */
    fun fields(): List<SiField> = schema().fields

    /**
     * Returns a list of fields that should be exposed in input forms and similar UI elements.
     * Not all fields (like vectors) make sense to edit by hand, so this method returns
     * only those fields that are appropriate for manual editing.
     *
     * @return List of fields that can be edited through forms
     */
    fun formFields(): List<SiField> = schema().formFields()

    /**
     * Retrieves the value associated with the given key of this search index record.
     * This method is used to convert native kotlin formatted record to a field-value map,
     * json object and lucene document.
     *
     * @param key The field for which the value is to be retrieved.
     * @return The value associated with the given key, or null if the key is not present.
     */
    fun getValue(key: SiField): Any?

    /**
     * Sets the value associated with the given key in this search index record.
     * This method is used to update the native kotlin structure of the record
     * from a json or lucene sources. It is used both in object creation and update.
     *
     * @param key The field for which the value is to be set.
     * @param value The new value to be associated with the given key.
     */
    fun setValue(key: SiField, value: Any?)

    /**
     * Converts the search index record to a map representation. This map contains
     * each field of the schema as keys, with their corresponding values, unless
     * the includeFields does not specify otherwise.
     *
     * @param includeFields The list of fields to include in the map. If null, all
     * fields defined by the schema are included.
     * @return A map where keys are fields from the schema and values are their
     * corresponding values from the record.
     */
    fun toMap(includeFields: List<SiField>? = null): Map<SiField, Any?> =
        (includeFields ?: fields()).associate {
            it to getValue(it)
        }

    /**
     * Converts the search index record to a JSON representation.
     * This method transforms each field of the schema into a JSON
     * object, including their corresponding values as key-value pairs.
     *
     * @param includeFields The list of fields to include in the JSON object.
     * If null, all fields defined by the schema are included.
     * @param shortenTo Shorten long values to this size
     * @return A JsonObject where keys are fields from the schema and values
     * are their corresponding values from the record.
     */
    fun toJson(includeFields: List<SiField>? = null, shortenTo: Int? = null): JsonObject =
        toMap(includeFields).entries.associate { (key, value) ->
            key to ConvertUtil.toJsonValue(value, shortenTo)
        }.let {
            JsonObject(it.map { it.key.primary() to it.value }.toMap())
        }


    /**
     * Converts the search index record to a Lucene Document.
     * This method transforms each field of the schema into Lucene fields and adds them to
     * the document.
     *
     * @param includeFields The list of fields to include in the Lucene Document. If null,
     * all fields defined by the schema are included.
     * @return A Lucene Document representing the record.
     */
    fun toLuceneDoc(includeFields: List<SiField>? = null): Document =
        Document().also { doc ->
            toMap(includeFields).forEach { (key, value) ->
                key.toLuceneFields(value).forEach {
                    doc.add(it)
                }
            }
        }


    /**
     * Converts the search index record to a log friendly string.
     * This method will transform each field of the schema into a human-readable string
     * representation suited for logging purposes. Fields of type FloatArray
     * and Collection are properly formatted for readability.
     *
     * @return A formatted string representing the fields and values of the record, suitable for logging.
     */
    fun toLogString(): String =
        toMap().mapNotNull { field ->
            val key = field.key.primary()
            val value = field.value.let { v ->
                @Suppress("UNCHECKED_CAST")
                when (v) {
                    null -> null
                    is FloatArray -> v.joinToString(", ") { it.toString() }
                    is Collection<*> -> (v as Collection<Any>).flatMap { it.toString().lines() }.joinToString("\n\t")

                    else -> v.toString().lines().joinToString("\n\t")
                }
            }
            value?.ifBlank { null }?.let {
                "$key: $value"
            }
        }.joinToString("\n").plus("\n")


    /**
     * Updates the current search index record using the given JSON object.
     * The method identifies fields in the schema, maps each field in the JSON
     * object to the schema field, and sets the corresponding value in the record.
     *
     * @param jsonObject The JSON object containing fields and their new values
     * to update the record with.
     * @return The updated search index record.
     */
    fun update(jsonObject: JsonObject): SiRecord {
        jsonObject.entries.mapNotNull { f ->
            schema().field(f.key)?.let {
                it to f.value
            }
        }.forEach { (key, value) ->
            when (value) {
                is JsonPrimitive -> this.setValue(key, value.contentOrNull)
                is JsonArray -> this.setValue(key, value.mapNotNull { it.jsonPrimitive.contentOrNull }.toList())
                else -> {}
            }
        }
        return this
    }


    companion object {
        /**
         * Defines a field for sorting records in the search index.
         * The SortKey will be indexed into this field, which is then used
         * for sorting search results.
         */
        val sortField = object : SiField {
            override fun fieldType(): Set<SiFieldType> = setOf(SiFieldType.MetaTags)
            override fun primary(): String = "sort"
        }


        /**
         * Converts a space-separated string of floating point numbers into a FloatArray.
         * This is used to parse vector representations from string format (e.g. from Lucene documents).
         *
         * @param s The string to parse, containing space-separated float values
         * @return FloatArray containing the parsed values, or null if input string is null
         */
        fun fromVectorString(s: String?): FloatArray? = s?.split(' ')?.map { it.toFloat() }?.toFloatArray()

        /**
         * Converts a FloatArray into a space-separated string of floating point numbers.
         * This is used to convert vector arrays into a string format for storage (e.g. in Lucene documents).
         *
         * @param a The FloatArray to convert
         * @return Space-separated string containing the float values
         */
        fun toVectorString(a: FloatArray): String = a.joinToString(" ") { it.toString() }

        val jsonEncoder = Json {
            isLenient = true
        }
    }
}
