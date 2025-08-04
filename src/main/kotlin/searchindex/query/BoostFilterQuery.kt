package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.tool.SiFilterClause
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

class BoostFilterQuery(val schema: SiSchema, val query: Query, val filterQueries: List<SiFilterClause>?) :
    SiQueryBuilder {
    constructor(schema: SiSchema, query: Query, filterQuery: Query) : this(
        schema,
        query,
        listOf(SiFilterClause(filterQuery))
    )


    override fun build(): Query =
        filterQueries?.ifEmpty { null }?.let { filterList ->
            BooleanQuery.Builder().also { builder ->
                builder.add(BooleanClause(query, BooleanClause.Occur.MUST))
                filterList.forEach { filter ->
                    builder.add(filter.asBoostQuery(), BooleanClause.Occur.SHOULD)
                }
            }.build()
        } ?: query
}


fun SiSchema.boostFilterQuery(q: Query, filterQueries: List<SiFilterClause>?): Query =
    BoostFilterQuery(this, q, filterQueries).build()

