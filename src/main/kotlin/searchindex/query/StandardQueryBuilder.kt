package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchIndex
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.Query

/**
 * Builder class for creating standard Lucene search queries using the classic QueryParser.
 *
 * This class provides a simple way to construct Lucene queries using standard query syntax.
 * It uses Lucene's StandardAnalyzer for text analysis and QueryParser for query construction.
 *
 * @property schema The schema defining the structure of the searchable documents
 * @property field The field to be searched, used as the default field for the query parser
 * @property phrase The search phrase to be parsed into a Lucene query
 *
 * @see org.apache.lucene.queryparser.classic.QueryParser
 * @see org.apache.lucene.analysis.standard.StandardAnalyzer
 */
class StandardQueryBuilder(val schema: SiSchema, val field: SiField, val phrase: String) : SiQueryBuilder {
    override fun build(): Query =  QueryParser(field.primary(), StandardAnalyzer()).parse(phrase)
}

/**
 * Constructs and executes a search query using the Lucene Standard Query Parser.
 *
 * This parser can handle a variety of query formats, providing flexibility in crafting search
 * queries. It supports boolean operators (AND, OR, NOT), wildcards, fuzzy searches,
 * and field-specific searches using the format "fieldname:value".
 *
 * Example usage:
 * ```
 * // Search in default field
 * index.standardQuery("searchterm")
 *
 * // Field-specific search
 * index.standardQuery("title:kotlin AND content:coroutines")
 * ```
 *
 * @param phrase The search phrase to parse. Can include boolean operators and field specifications.
 * @param field The field to search in if no specific field is mentioned in the query string.
 *             Defaults to the schema's default search field.
 * @return A [SiHits] object containing the search results matching the query.
 */
fun <TT: SiRecord> SiSearchInterface<TT>.standardQuery(phrase: String, field: SiField = schema.defaultSearchField): SiHits<TT> =
    StandardQueryBuilder(
        this.schema, this.schema.defaultSearchField, phrase
    ).build().let {
        this.search(it) as SiHits<TT>
    }
