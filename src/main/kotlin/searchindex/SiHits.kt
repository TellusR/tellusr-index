package com.tellusr.searchindex

import org.apache.lucene.search.TotalHits


/**
 * SiHits class representing a structure for storing search results.
 *
 * @property docs list of records
 * @property totalHists number of matches in the index. Might be larger than the number of records in docs
 */
data class SiHits<T : SiRecord>(val totalHits: TotalHits, val docs: List<T>) {
    fun isEmpty() = docs.isEmpty()
    fun isNotEmpty() = !isEmpty()
    fun size() = docs.size
}
