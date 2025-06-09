package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiFieldType
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.index.Term
import org.apache.lucene.search.MatchNoDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery


/**
 * Builds a Lucene query to search for documents by their key field.
 *
 * This query builder creates a [TermQuery] that matches documents containing an exact term
 * in a specified field. If no field is provided, it attempts to find the first field of type [SiFieldType.ForeignKey]
 * in the schema. If no suitable field is found, returns a [MatchNoDocsQuery].
 *
 * @property schema The schema containing field definitions
 * @property field Optional specific field to search in, if null will use first ForeignKey field
 * @property key The key value to search for
 */
class KeyQueryBuilder(
    private val schema: SiSchema, private val field: SiField?, private val key: String
) : SiQueryBuilder {
    override fun build(): Query {
        return (field ?: schema.fields.firstOrNull {
            it.isType(SiFieldType.ForeignKey)
        })?.let { field ->
            TermQuery(Term(field.primary(), key))
        } ?: MatchNoDocsQuery()
    }
}


/**
 * Searches for documents by matching an exact term in a specified field, typically used for foreign key lookups.
 *
 * This method is particularly useful when you need to find documents that reference other documents
 * through foreign key relationships. It performs an exact match search, meaning the term must match
 * the field value precisely.
 *
 * Example usage:
 * ```
 * // Find all documents referencing parentId "123" in the default ID field
 * val hits = index.keySearch("123")
 *
 * // Find documents with specific category
 * val hits = index.keySearch("technology", Fields.Category)
 * ```
 *
 * @param TT The type of record to be returned in the search results
 * @param phrase The exact term to match against the specified field
 * @param field The field to search within, defaults to the schema's ID field
 * @return A [SiHits] object containing the matching documents and search metadata
 */
fun <TT : SiRecord> SiSearchInterface<TT>.keySearch(phrase: String, field: SiField = schema.idField): SiHits<TT> = KeyQueryBuilder(
    this.schema, field, phrase
).build().let {
    this.search(it) as SiHits<TT>
}

