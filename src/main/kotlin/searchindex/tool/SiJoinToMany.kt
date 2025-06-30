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


    private suspend fun toOneToMany(one: T1) =
        (one.getValue(primary.schema.idField) as String?)?.let {
            val many = secondary.keySearch(it, foreignKeyField).docs
            SiOneToMany<T1, T2>(one, many)
        }


    override suspend fun size(): Int = primary.size()

    override suspend fun all(start: Int, rows: Int): SiHits<SiOneToMany<T1, T2>> {
        val hits = primary.all(start, rows)
        val joined = hits.docs.mapNotNull {
            toOneToMany(it)
        }
        return SiHits(hits.totalHits, joined)
    }

    override val schema: SiSchema = SiCombinedSchema(primary.schema, secondary.schema)


    override suspend fun search(query: Query, start: Int, rows: Int, sort: Sort?): SiHits<SiOneToMany<T1, T2>> {
        val hits = primary.search(query,  start, rows, sort)
        val joined = hits.docs.mapNotNull { one ->
            toOneToMany(one)
        }
        return SiHits(hits.totalHits, joined)
    }

    override suspend fun byId(q: String): SiOneToMany<T1, T2>? = primary.byId(q)?.let {
        toOneToMany(it)
    }
}
