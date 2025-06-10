package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.tool.SiFilterClause
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import kotlin.collections.forEach

class FilterQueryBuilder(val schema: SiSchema, val query: Query, val filterQueries: List<SiFilterClause>?) :
    SiQueryBuilder {
    constructor(schema: SiSchema, query: Query, filterQuery: Query) : this(
        schema,
        query,
        listOf(SiFilterClause(filterQuery))
    )

    override fun build(): Query {
        return filterQueries?.ifEmpty { null }?.let { list ->
            BooleanQuery.Builder().also { b ->
                b.add(BooleanClause(query, BooleanClause.Occur.MUST))
                list.forEach { searchFilter ->
                    b.add(searchFilter.query, searchFilter.clause)
                }
            }.build()
        } ?: query
    }
}

fun SiSchema.filterQuery(query: Query, filterQuery: Query?): Query = filterQuery?.let { f ->
    FilterQueryBuilder(
        this, query, f
    ).build()
} ?: query


fun SiSchema.filterQuery(query: Query, filter: String?): Query = filter?.let { f ->
    FilterQueryBuilder(
        this, query, StandardQueryBuilder(this, this.defaultSearchField, f).build()
    ).build()
} ?: query


fun SiSchema.filterQuery(q: Query, filterQueries: List<SiFilterClause>?) = filterQueries?.ifEmpty {
    null
}?.let {
    FilterQueryBuilder(
        this, q, it
    )
}


