package com.tellusr.searchindex.query

import com.tellusr.searchindex.TestStore
import com.tellusr.searchindex.util.getAutoNamedLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTest {
    private lateinit var index: TestStore.SearchIndex
    private val testIndexName = "test-index"

    @BeforeAll
    fun setup() {
        index = TestStore.SearchIndex(testIndexName)
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            index.clear()
            File(testIndexName).deleteRecursively()
        }
    }


    @Test
    fun `test phrase search`() {
        runBlocking {
            val id = "phrase1"
            index.add(
                TestStore.Record(id, "this is an exact phrase", "context", "response", null, null)
            )

            logger.info(index.all().docs.joinToString("\n") { it.toLogString() })

            val results = index.phraseSearch("this is an exact phrase", TestStore.Fields.Instruction)

            assertTrue(results.isNotEmpty())
            assertEquals(id, results.docs.first().id)
        }
    }

    @Test
    fun `test phrase search order matters`() {
        runBlocking {
            val id = "phrase2"
            index.add(TestStore.Record(id, "first second third", "context", "response", null, null))

            val matchingResults = index.phraseSearch("first second", TestStore.Fields.Instruction)
            val nonMatchingResults = index.phraseSearch("second first", TestStore.Fields.Instruction)

            assertTrue(matchingResults.isNotEmpty())
            assertTrue(nonMatchingResults.isEmpty())
        }
    }

    @Test
    fun `test term search exact match`() {
        runBlocking {
            val id = "term1"
            index.add(TestStore.Record(id, "specific term search", "context", "response", null, null))

            val results = index.termQuery("term", TestStore.Fields.Instruction)

            assertTrue(results.isNotEmpty())
            assertEquals(id, results.docs.first().id)
        }
    }

    @Test
    fun `test term search case insensitivity`() {
        runBlocking {
            val id = "term2"
            index.add(TestStore.Record(id, "UPPERCASE term", "context", "response", null, null))

            val upperResults = index.termQuery("UPPERCASE", TestStore.Fields.Instruction)
            val lowerResults = index.termQuery("uppercase", TestStore.Fields.Instruction)

            assertTrue(upperResults.isNotEmpty())
            assertTrue(lowerResults.isNotEmpty())
        }
    }

    @Test
    fun `test exact query match`() {
        runBlocking {
            val id = "exact1"
            index.add(TestStore.Record(id, "exact match test", "context", "response", null, null))

            val results = index.exactSearch("exact match test", TestStore.Fields.Instruction)

            assertTrue(results.isNotEmpty())
            assertEquals(id, results.docs.first().id)
        }
    }

    @Test
    fun `test exact query case sensitivity`() {
        runBlocking {
            val id = "exact2"
            index.add(TestStore.Record(id, "Exact Case Test", "context", "response", null, null))

            val matchingResults = index.exactSearch("Exact Case Test", TestStore.Fields.Instruction)
            val nonMatchingResults = index.exactSearch("case unexact test", TestStore.Fields.Instruction)

            assertTrue(matchingResults.isNotEmpty())
            assertTrue(nonMatchingResults.isEmpty())
        }
    }

    @Test
    fun `test exact query no match`() {
        runBlocking {
            val id = "exact3"
            index.add(TestStore.Record(id, "some other text", "context", "response", null, null))

            val results = index.exactSearch("non existing text", TestStore.Fields.Instruction)

            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun `test key query on category field`() {
        runBlocking {
            val id = "category1"
            index.add(TestStore.Record(id, "instruction", "context", "response", "test-category", null))

            val results = index.keySearch("test-category", TestStore.Fields.Category)

            assertTrue(results.isNotEmpty())
            assertEquals(id, results.docs.first().id)
        }
    }

    @Test
    fun `test vector query similarity search`() {
        runBlocking {
            val id = "vector1"
            val vector = FloatArray(128) { 0.1f }  // Sample vector
            index.add(TestStore.Record(id, "vector test", "context", "response", null, null, null, vectorized = vector))

            val searchVector = FloatArray(128) { 0.1f }  // Similar vector for search
            val results = index.vectorQuery(searchVector, TestStore.Fields.Vectorized)

            assertTrue(results.isNotEmpty())
            assertEquals(id, results.docs.first().id)
        }
    }

    companion object {
        private val logger = getAutoNamedLogger()
    }
}
