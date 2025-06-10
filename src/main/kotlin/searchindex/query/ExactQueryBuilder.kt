package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.search.Query

/**
 * Builds exact match queries for both keyword and non-keyword fields.
 * For keyword fields, it uses KeyQueryBuilder, otherwise PhraseQueryBuilder.
 *
 * @property schema The schema containing field definitions
 * @property field The field to search in
 * @property q The query string to match exactly
 */
class ExactQueryBuilder(val schema: SiSchema, val field: SiField, val q: String): SiQueryBuilder {
    /**
     * Builds the exact match query based on field type.
     *
     * @return Query A Lucene query for exact matching
     */
    override fun build(): Query = if (field.isKeywordField()) {
        KeyQueryBuilder(schema, field, q).build()
    } else {
        PhraseQueryBuilder(schema, field, q).build()
    }
}


/**
 * Performs an exact match search on the specified field.
 *
 * @param q The query string to match exactly
 * @param field The field to search in, defaults to schema's default search field
 * @return SiHits<TT> Search results containing matching records
 */
fun <TT: SiRecord> SiSearchInterface<TT>.exactSearch(q: String, field: SiField = schema.defaultSearchField): SiHits<TT> =
    ExactQueryBuilder(
        this.schema, field, q
    ).build().let {
        this.search(it) as SiHits<TT>
    }

fun SiSchema.exactQuery(q: String, field: SiField = this.defaultSearchField): Query =
    ExactQueryBuilder(
        this, field, q
    ).build()