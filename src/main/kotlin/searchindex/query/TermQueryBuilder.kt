package com.tellusr.searchindex.query

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiHits
import com.tellusr.searchindex.SiQueryBuilder
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSchema
import com.tellusr.searchindex.SiSearchInterface
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery

class TermQueryBuilder(
    private val schema: SiSchema,
    private val field: SiField,
    private val phrase: String,
    private val clause: BooleanClause.Occur = BooleanClause.Occur.SHOULD
) :
    SiQueryBuilder {
    val queries: List<Query> = SiQueryBuilder.tokenize(schema, field, phrase).map {
        if (it.text().endsWith("*"))
            WildcardQuery(it)
        else
            TermQuery(it)
    }

    override fun build(): Query = BooleanQueryBuilder(queries, clause).build()
}

suspend fun <TT : SiRecord> SiSearchInterface<TT>.termSearch(
    phrase: String,
    field: SiField = schema.defaultSearchField,
    clause: BooleanClause.Occur = BooleanClause.Occur.SHOULD
): SiHits<TT> =
    TermQueryBuilder(
        this.schema, field, phrase, clause
    ).build().let {
        this.search(it) as SiHits<TT>
    }

fun SiSchema.termQuery(
    phrase: String,
    field: SiField = this.defaultSearchField,
    clause: BooleanClause.Occur = BooleanClause.Occur.SHOULD
): Query = TermQueryBuilder(
    this, field, phrase, clause
).build()
