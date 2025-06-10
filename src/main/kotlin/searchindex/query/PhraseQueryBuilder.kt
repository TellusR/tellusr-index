package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.Query

/**
 * Builds a Lucene PhraseQuery for exact phrase matching within a specific field.
 *
 * This query builder creates a phrase query that matches documents containing
 * the exact sequence of terms in the specified field. The terms are tokenized
 * using the schema's analyzer before building the query.
 *
 * @property schema The schema containing field definitions and analyzers
 * @property field The field to search within
 * @property phrase The exact phrase to search for
 */
class PhraseQueryBuilder(
    private val schema: SiSchema,
    private val field: SiField,
    private val phrase: String
) : SiQueryBuilder {

    override fun build(): Query {
        return PhraseQuery.Builder().also { phraseQuery ->
            SiQueryBuilder.tokenize(schema, field, phrase).forEachIndexed { index, term ->
                phraseQuery.add(term, index + 1)
            }
        }.build()
    }
}

/**
 * Constructs a `PhraseQuery` for searching an exact sequence of terms within
 * a specified field and returns matching records.
 *
 * It utilizes the schema's analyzer to tokenize the input string, and builds
 * a phrase query that ensures the tokens appear in the same sequence.
 *
 * @param phrase The exact phrase to search for within documents.
 * @param field The field within the index to search in, defaults to schema's default search field.
 * @return A `SiHits<TT>` containing the matching records of type TT.
 * @throws IllegalArgumentException if the field does not exist in schema
 * @throws IllegalStateException if the index is not readable
 */
fun <TT: SiRecord> SiSearchInterface<TT>.phraseSearch(phrase: String, field: SiField = schema.defaultSearchField): SiHits<TT> =
    PhraseQueryBuilder(
        this.schema, field, phrase
    ).build().let {
        this.search(it) as SiHits<TT>
    }


fun SiSchema.phraseQuery(
    phrase: String,
    field: SiField = this.defaultSearchField
): Query = PhraseQueryBuilder(
    this, field, phrase
).build()
