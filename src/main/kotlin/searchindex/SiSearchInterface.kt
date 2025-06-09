package com.tellusr.searchindex

import com.tellusr.searchindex.exception.TellusRIndexException
import org.apache.lucene.search.Query
import org.apache.lucene.search.Sort


/**
 * SiSearchInterface defines a contract for searching functionalities.
 * It is implemented by SearchIndex as well as the joins SiJoinToMany and
 * SiJoinToOther, and is useful for implementing interfaces that could
 * support any of them.
 */
interface SiSearchInterface<TT : SiRecord> {
    
    /**
     * Retrieves all records from the search index with pagination support.
     *
     * @param page The page number to retrieve (zero-based). Defaults to 0.
     * @param pageSize The maximum number of records per page. Defaults to SiSchema.ALL.
     * @return A SiHits object containing the matched records for the requested page.
     */
    fun all(page: Int = 0, pageSize: Int = SiSchema.ALL): SiHits<TT>

    /**
     * Returns the total number of records in the index.
     *
     * @return The number of records currently stored in the index.
     */
    val size: Int

    /**
     * Searches for a record by its unique identifier.
     *
     * @param q The unique identifier of the record to search for.
     * @return The record that matches the given identifier, or null if no such record exists.
     */
    fun byId(q: String): TT?

    /**
     * Searches for records that match the given query criteria.
     *
     * @param query An object representing the search criteria.
     * @param pageSize Max number of records to return
     * @return A list of records matching the search criteria.
     */
    fun search(query: Query, page: Int = 0, pageSize: Int = SiSchema.ALL, sort: Sort? = schema.defaultSort): SiHits<TT>


    /**
     * Checks if a record exists in the index based on its unique ID.
     * If the record's ID is null, it means the ID hasn't been auto-created yet
     * (which happens during add or update operations), so the record cannot exist
     * in the index.
     *
     * @param record The record to check for existence
     * @return true if the record exists in the index, false otherwise
     */
    suspend fun exists(record: SiRecord): Boolean = record.uniqueId()?.let { id ->
        byId(id) != null
    } ?: false


    /**
     * Adds a list of records to the search index. Each record must not already exist in the index.
     * This method first validates that none of the records already exist, then updates the index with all records.
     *
     * @param records The list of records to add to the index.
     * @throws TellusRIndexException.AlreadyExistsException If any of the records already exists in the index
     *         (based on their unique IDs).
     */
    suspend fun add(records: List<TT>) {
        // Validate
        records.forEach { r ->
            if (exists(r))
                throw TellusRIndexException.AlreadyExistsException(
                    "Document with id ${r.uniqueId()} already exists"
                )
        }

        // update will insert non-existing records
        update(records)
    }

    suspend fun add(record: TT) = add(listOf(record))
    suspend fun add(vararg record: TT) = add(record.toList())

    suspend fun update(records: List<SiRecord>)
    suspend fun update(record: SiRecord) {
        update(listOf(record))
    }
    suspend fun update(vararg record: SiRecord) = update(record.toList())

    suspend fun remove(idList: List<String>)
    suspend fun remove(id: String) = remove(listOf(id))
    suspend fun remove(vararg idList: String) = remove(idList.toList())

    suspend fun clear() {}

    val schema: SiSchema
}
