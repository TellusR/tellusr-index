package com.tellusr.searchindex.tool

import com.tellusr.searchindex.*
import org.apache.lucene.search.Query
import org.apache.lucene.search.Sort


open class SiJoinToOther<T1: SiRecord, T2: SiRecord>(
    val primary: SiSearchIndex<T1>,
    val secondary: SiSearchIndex<T2>,
    val foreignKeyField: SiField
) : SiSearchInterface<SiOneToOther<T1, T2>> {
    override fun all(start: Int, rows: Int): SiHits<SiOneToOther<T1, T2>> {
        val hits = primary.all(rows)
        val joined = hits.docs.mapNotNull {
            toOneToOther(it)
        }
        return SiHits<SiOneToOther<T1, T2>>(hits.totalHits, joined)
    }

    override val size: Int get() = primary.size


    private fun toOneToOther(one: T1): SiOneToOther<T1, T2>? = (one.getValue(foreignKeyField) as String?)?.let { foreignKey ->
        val other = secondary.byId(foreignKey)
        SiOneToOther<T1, T2>(one, other)
    }

    fun toOneToOtherList(oneList: List<T1>): List<SiOneToOther<T1, T2>> =
        oneList.mapNotNull { one -> toOneToOther(one) }


    override fun search(query: Query, start: Int, rows: Int, sort: Sort?): SiHits<SiOneToOther<T1, T2>> {
        val hits = primary.search(query, start, rows, sort)
        val joined = toOneToOtherList(hits.docs)
        return SiHits(hits.totalHits, joined)
    }


    override suspend fun remove(idList: List<String>) {
        primary.remove(idList)
    }

    override suspend fun clear() {
        primary.clear()
        secondary.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun update(records: List<SiRecord>) {
        records.forEach { record ->
            (record as? SiOneToOther<T1, T2>)?.let { rec ->
                primary.update(rec.one)
                rec.other?.let { secondary.update(it) }
            }
        }
    }

    override fun byId(q: String): SiOneToOther<T1, T2>? = primary.byId(q)?.let {
        toOneToOther(it)
    }


    override val schema: SiSchema = SiCombinedSchema(primary.schema, secondary.schema)
}
