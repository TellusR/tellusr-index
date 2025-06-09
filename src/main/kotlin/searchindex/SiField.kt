package com.tellusr.searchindex

import com.tellusr.searchindex.tool.SiConstraint
import org.apache.lucene.index.IndexableField
import java.util.*

/**
 * SiField represents a single field within the database schema, including its name and
 * type. This interface is usually implemented by an enum class, where each enum value
 * represents a field.
 */
interface SiField {
    /**
     * Converts the field representation to its corresponding Lucene field name.
     * This name is used as the identifier when indexing and querying in Lucene.
     */
    fun primary(): String

    /**
     * Generates the secondary field name for the current field. The secondary
     * field name typically serves as a json key when a record is joined with another
     * in a join operation to avoid name conflicts if both sides of the join
     * contains identical field names.
     *
     * @return A string representing the secondary field name,
     * by default the lucene field name prefixed by "b_".
     */
    fun secondary(): String = "b_${primary()}"


    /**
     *
     */
    fun capitalized(): String =
        primary().replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(Locale.getDefault())
            else
                it.toString()
        }



    /**
     * Retrieves the set of types associated with the field.
     * This method is used to determine the characteristics and behavior of the field within
     * the schema.
     *
     * The method is idiomatically implemented with a when that gives a compiler error
     * if a field is not associated with a field type.
     *
     * @return A set of SiFieldType enumerations, representing the types applicable to this field.
     */
    fun fieldType(): Set<SiFieldType>

    fun constraints(): List<SiConstraint> = listOf()

    /**
     * Determines whether the field is of the specified type. This is achieved by checking if
     * the set of types associated with the field contains the provided type.
     *
     * @param type The type to be checked against the field's types.
     * @return True if the field's types contain the specified type, otherwise false.
     */
    fun isType(type: SiFieldType): Boolean = fieldType().contains(type)

    /**
     * Checks if the current field is a keyword-type field.
     *
     * This method evaluates whether the field is of type Keyword, UniqueId, or ForeignKey,
     * which helps in determining the appropriate handling necessary for indexing and querying.
     *
     * @return True if the field type is Keyword, UniqueId, or ForeignKey, otherwise false.
     */
    fun isKeywordField(): Boolean = isType(SiFieldType.Keyword)
            || isType(SiFieldType.UniqueId)
            || isType(SiFieldType.ForeignKey)

    /**
     * Converts the field and its associated value to a list of Lucene fields.
     *
     * This method processes the field's types and generates a corresponding list of
     * one or more Lucene fields for indexing, based on the provided value. This method
     * is not usually called directly, except by the toLuceneDoc method in the SiRecord
     * class 
     *
     * @param value The value to be indexed. It can be of any type.
     * @return A list of IndexableField objects, each representing an individual Lucene field.
     */
    fun toLuceneFields(value: Any?): List<IndexableField> =
        fieldType().flatMap {
            it.toLuceneFields(primary(), value)
        }
}
