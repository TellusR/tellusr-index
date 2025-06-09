package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiQueryBuilder
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

/**
 * Builds a Boolean query that combines multiple Lucene queries using specified occurrence clause.
 * This builder supports creating compound queries where multiple conditions are combined using
 * boolean logic (MUST, SHOULD, MUST_NOT).
 *
 * @property queries List of Lucene queries to be combined in the boolean query
 * @property clause The occurrence clause that specifies how queries should be combined (defaults to SHOULD)
 */
class BooleanQueryBuilder(
    val queries: List<Query>,
    val clause: BooleanClause.Occur = BooleanClause.Occur.SHOULD
): SiQueryBuilder {
    /**
     * Builds and returns a combined Boolean query from the provided queries and clause.
     *
     * @return A Lucene Query object representing the combined boolean query
     */
    override fun build(): Query {
        return BooleanQuery.Builder().also { builder ->
            queries.forEach { query ->
                builder.add(query, clause)
            }
        }.build()
    }
}
