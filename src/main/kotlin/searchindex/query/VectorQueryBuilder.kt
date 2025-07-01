package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiFieldType
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.search.MatchNoDocsQuery
import org.apache.lucene.search.Query

/**
 * Builder class for creating vector-based search queries using KNN (k-nearest neighbors) algorithm.
 *
 * @property schema The schema defining the index structure and fields
 * @property field Optional specific field to search in, if null the first vector field will be used
 * @property vectorQuery The float array vector to use for similarity search
 * @property maxRows Maximum number of results to return, defaults to 10
 */
class VectorQueryBuilder(
    val schema: SiSchema,
    val field: SiField?,
    val vectorQuery: FloatArray,
    val maxRows: Int = 10,
    val filterQuery: Query? = null,
): SiQueryBuilder {
    /**
     * Builds and returns a Lucene query for vector similarity search.
     *
     * @return A KnnFloatVectorQuery if a valid vector field is found, otherwise returns MatchNoDocsQuery
     */
    override fun build(): Query {
        return (field ?: schema.fields.firstOrNull {
            it.isType(SiFieldType.Vector)
        })?.let { vectorField ->
            filterQuery?.let { filter ->
                KnnFloatVectorQuery(vectorField.primary(), vectorQuery, maxRows, filter)
            } ?: KnnFloatVectorQuery(vectorField.primary(), vectorQuery, maxRows)
        } ?: MatchNoDocsQuery()
    }
}

/**
 * Constructs a KNN (k-nearest neighbors) query for searching similar vectors in the
 * specified field and executes the search.
 *
 * @param vectorSearch The float array vector used for the KNN search.
 * @param field Optional field within the index to apply the KNN search. If not specified,
 *             the first vector field in the schema will be used.
 * @param maxRows The maximum number of similar vectors to retrieve, default is 10.
 * @return A [SiHits] object containing the search results with vector similarity matches.
 */
suspend fun <TT: SiRecord> SiSearchInterface<TT>.vectorSearch(vectorQuery: FloatArray, field: SiField? = null, maxRows: Int = 10, filterQuery: Query? = null): SiHits<TT> =
    VectorQueryBuilder(
        this.schema, field, vectorQuery, maxRows, filterQuery
    ).build().let {
        this.search(it) as SiHits<TT>
    }


fun SiSchema.vectorQuery(vectorQuery: FloatArray, field: SiField? = null, maxRows: Int = 10, filterQuery: Query? = null): Query =
    VectorQueryBuilder(
        this, field, vectorQuery, maxRows, filterQuery
    ).build()
