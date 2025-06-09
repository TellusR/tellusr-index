package com.tellusr.searchindex.tool

import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.Query

class SiFilterClause(val query: Query, val clause: BooleanClause.Occur = BooleanClause.Occur.FILTER) {
    constructor(query: Query, shouldInclude: Boolean) : this(
        query,
        if (shouldInclude)
            BooleanClause.Occur.FILTER
        else
            BooleanClause.Occur.MUST_NOT
    )

    fun asBoostQuery(): Query {
        val boost: Float = when(clause) {
            BooleanClause.Occur.FILTER -> 8.0f
            BooleanClause.Occur.MUST -> 2.0f
            BooleanClause.Occur.MUST_NOT -> 0.1f
            BooleanClause.Occur.SHOULD -> 1.0f
        }
        return BoostQuery(query, boost)
    }
}

