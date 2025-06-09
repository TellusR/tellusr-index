package com.tellusr.searchindex.tool

import com.tellusr.searchindex.*
import com.tellusr.searchindex.exception.TellusRIndexException
import com.tellusr.searchindex.query.keySearch
import org.apache.lucene.search.Query
import org.apache.lucene.search.Sort

open class SiJoinToMany<T1 : SiRecord, T2 : SiRecord>(
    val primary: SiSearchIndex<T1>, val secondary: SiSearchIndex<T2>, val foreignKeyField: SiField
) : SiSearchInterface<SiOneToMany<T1, T2>> {
    override suspend fun remove(idList: List<String>) {
        throw TellusRIndexException.UnsupportedOperationException("Not applicable for ${this::class.simpleName}")
    }

    override suspend fun update(records: List<SiRecord>)  {
        throw TellusRIndexException.UnsupportedOperationException("Not applicable for ${this::class.simpleName}")
    }


    private fun toOneToMany(one: T1) =
        (one.getValue(primary.schema.idField) as String?)?.let {
            val many = secondary.keySearch(it, foreignKeyField).docs
            SiOneToMany<T1, T2>(one, many)
        }


    override val size: Int get() = primary.size

    override fun all(page: Int, pageSize: Int): SiHits<SiOneToMany<T1, T2>> {
        val hits = primary.all(pageSize)
        val joined = hits.docs.mapNotNull {
            toOneToMany(it)
        }
        return SiHits(hits.totalHits, joined)
    }

    override val schema: SiSchema = SiCombinedSchema(primary.schema, secondary.schema)


    override fun search(query: Query, page: Int, pageSize: Int, sort: Sort?): SiHits<SiOneToMany<T1, T2>> {
        val hits = primary.search(query,  page, pageSize, sort)
        val joined = hits.docs.mapNotNull { one ->
            toOneToMany(one)
        }
        return SiHits(hits.totalHits, joined)
    }

    override fun byId(q: String): SiOneToMany<T1, T2>? = primary.byId(q)?.let {
        toOneToMany(it)
    }
}
