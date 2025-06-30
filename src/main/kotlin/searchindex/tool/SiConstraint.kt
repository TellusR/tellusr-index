package com.tellusr.searchindex.tool

import com.tellusr.searchindex.exception.TellusRIndexException
import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiRecord
import com.tellusr.searchindex.SiSearchIndex
import com.tellusr.searchindex.query.exactSearch
import com.tellusr.searchindex.util.getAutoNamedLogger

interface SiConstraint {
    val name: String
    suspend fun validate(searchIndex: SiSearchIndex<*>): Boolean
    suspend fun mayInsert(record: SiRecord, searchIndex: SiSearchIndex<*>): Boolean = true

    suspend fun validateWithThrow(searchIndex: SiSearchIndex<*>) {
        if (validate(searchIndex)) {
            throw TellusRIndexException.ConstraintException(
                "Constraint $name error in ${searchIndex.qualifiedName()}"
            )
        }
    }
}


class SiConstraintUnique(val field: SiField) : SiConstraint {
    override suspend fun validate(searchIndex: SiSearchIndex<*>): Boolean = countDuplicates(searchIndex) > 0

    override val name: String
        get() = "unique"


    private suspend fun countDuplicates(searchIndex: SiSearchIndex<*>): Int =
        searchIndex.all().docs.map {
            it.getValue(field)
        }.groupBy {
            it
        }.filter {
            it.value.size > 1
        }.let { duplicates ->
            logger.warn("Failed $name constraint in ${searchIndex.qualifiedName()} for values: ${duplicates.keys.joinToString(", ")}")
            // Return number of records that are duplicates
            duplicates.values.sumOf { it.size } - duplicates.size
        }


    override suspend fun mayInsert(record: SiRecord, searchIndex: SiSearchIndex<*>): Boolean {
        val fieldValue = record.getValue(field)
        val matches = searchIndex.exactSearch(fieldValue.toString(), field)
        return matches.docs.firstOrNull {
            val v = it.getValue(field)
            v == fieldValue
        }.let {
            it != null
        }
    }


    companion object {
        val logger = getAutoNamedLogger()
    }
}
