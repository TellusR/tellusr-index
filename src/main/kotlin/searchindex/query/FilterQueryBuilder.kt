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
