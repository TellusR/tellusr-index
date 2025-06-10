package com.tellusr.searchindex

import com.tellusr.searchindex.query.StandardQueryBuilder
import com.tellusr.searchindex.query.TermQueryBuilder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.util.UUID


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CRUDTest {
    private lateinit var index: TestStore.SearchIndex
    private val testIndexName = "test-index-crud"

    @BeforeAll
    fun setup() {
        index = TestStore.SearchIndex(testIndexName)
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            index.clear()
            index.close()
            TestStore.Schema.path(testIndexName).toFile().deleteRecursively()
        }
    }

    @Test
    fun `test adding single record`() = runBlocking {
        val record = TestStore.Record(
            id = "1",
            instruction = "test instruction",
            context = "test context",
            response = "test response",
            category = null,
            language = null
        )

        index.add(record)
        val retrieved = index.byId("1")

        assertNotNull(retrieved)
        assertEquals("1", retrieved?.id)
        assertEquals("test instruction", retrieved?.instruction)
    }

    @Test
    fun `test bulk add records`() = runBlocking {
        val records = listOf(
            TestStore.Record("2", "instruction2", "context2", "response2", null, null),
            TestStore.Record("3", "instruction3", "context3", "response3", null, null)
        )

        index.update(records)

        val all = index.all().docs
        assertTrue(all.size >= 2)
    }

    @Test
    fun `test update record`() = runBlocking {
        val id = "4"
        index.add(TestStore.Record(id, "original", "context", "response", null, null))

        index.update(id) { record ->
            record.instruction = "updated"
        }

        val updated = index.byId(id)
        assertEquals("updated", updated?.instruction)
    }

    @Test
    fun `test delete record`() = runBlocking {
        val id = "5"
        index.add(TestStore.Record(id, "to delete", "context", "response", null, null))

        index.remove(id)

        val deleted = index.byId(id)
        assertNull(deleted)
    }

    @Test
    fun `test search by instruction`() {
        runBlocking {
            index.update(TestStore.Record("6", "searchable text", "context", "response", null, null))

            val query = TermQueryBuilder(
                index.schema,
                TestStore.Fields.Instruction,
                "searchable"
            ).build()

            val results = index.search("Instruction:searchable")

            assertTrue(results.isNotEmpty())
            assertEquals("6", results.docs.first().id)
        }
    }

    @Test
    fun `test search with pagination`() = runBlocking {
        val records = (7..16).map {
            TestStore.Record(it.toString(), "page test", "context", "response", null, null)
        }
        index.update(records)

        val query = StandardQueryBuilder(
            index.schema,
            TestStore.Fields.Instruction,
            "Instruction:\"page test\""
        ).build()

        val page = index.search(query, pageSize = 3)

        assertEquals(3, page.size())
    }
}
