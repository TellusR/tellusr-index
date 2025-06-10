package com.tellusr.searchindex

import com.tellusr.searchindex.tool.SiConstraint
import kotlinx.serialization.json.*
import com.tellusr.searchindex.util.getAutoNamedLogger
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute
import org.apache.lucene.document.*
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import java.nio.file.Path
import java.nio.file.Paths


/**
 * SiSchema represents the structure and properties of a search index.
 *
 * @param group The group name of the schema. Combined with the name of the SearchIndex it provides
 *   a unique identifier that may be used in file paths.
 * @param fields A list of fields, usually the entries of an enum that implements the SiField interface.
 * @param dir Root directory that index directories shuold reside inside.
 *
 * @constructor Creates a new instance of SiSchema with the specified properties.
 */
abstract class SiSchema(
    val group: String,
    val fields: List<SiField>
) {
    open val basePath: String? get() = defaultIndexPath

    open fun formFields(): List<SiField> = fields

    private val dir: String
        get() = basePath?.let {
            "$it/index"
        } ?: "index"

    /**
     * Constructs a Path for the seach index.
     * The method is used by SearchIndex class when accessing index files.
     *
     * @param name The base name of the index file.
     * @return A Path object pointing to the index file location.
     */
    fun path(name: String): Path = Paths.get("$dir/${name.lowercase()}/$group.idx")

    fun oldPath(name: String): Path = (basePath?.let {
        "$it/lucene/index"
    } ?: "lucene/index").let { dir ->
        Paths.get("$dir/$group/${name.lowercase()}.idx")
    }


    /**
     * Converts a Lucene Document into an instance of SiRecord.
     * This factory method must be implemented for each subclass of SiSchema / SiRecord.
     * By convention it is implemented as constructor to the SiRecord subclass, which
     * is used by by the override of this method.
     *
     * @param doc The Lucene Document to be converted.
     * @return An instance of SiRecord representing the data contained in the Document.
     */
    abstract fun fromDoc(doc: Document): SiRecord


    /**
     * Converts the provided JsonObject into an instance of SiRecord.
     * This factory method must be implemented for each subclass of SiSchema / SiRecord.
     * By convention it is implemented as constructor to the SiRecord subclass, which
     * is used by by the override of this method.
     *
     * @param jsonObject The JsonObject to be converted.
     * @return An instance of SiRecord representing the data contained in the JsonObject.
     */
    abstract fun fromJson(jsonObject: JsonObject): SiRecord


    /**
     * Returns the SiField corresponding to the given name.
     *
     * @param name The name of the field to search for.
     * @return The SiField that matches the given name, or null if no
     *   match is found. The comparison is case-insensitive.
     */
    fun field(name: String): SiField? = fields.firstOrNull {
        it.primary() == name
    }


    /**
     * The fields that are identified as keyword fields.
     * Keyword fields are those that require exact matching rather than analysis or stemming.
     * This includes SiFieldTypes UniqueId, Keyword, and ForeignKey.
     */
    val keywordFields: List<SiField> = fields.filter { it.isKeywordField() }


    /**
     * Represents the unique identifier field within the schema.
     *
     * Searches through the list of fields to find the first field that is of
     * type `UniqueId`. This field is used to uniquely identify records.
     */
    val idField: SiField = fields.first {
        it.isType(SiFieldType.UniqueId)
    }

    /**
     * Represents the default field to be used for search operations when no specific field is specified.
     *
     * The field is determined by the following precedence:
     * 1. First field marked with SiFieldType.DefaultSearch
     * 2. If no DefaultSearch field exists, first field marked with SiFieldType.Text
     * 3. If no Text field exists, falls back to the idField
     *
     * This field is used as the default target for search queries where no explicit field
     * is specified in the search criteria.
     */
    val defaultSearchField: SiField = fields.firstOrNull {
        it.isType(SiFieldType.DefaultSearch)
    } ?: fields.firstOrNull {
        it.isType(SiFieldType.Text)
    } ?: idField


    val constraints: List<SiConstraint> = fields.flatMap { it.constraints() }


    /**
     * Provides the default analyzer that will be used by the indexing system.
     * Override this method to provide a custom default analyzer for the
     * analyze method.
     *
     * @return An instance of `Analyzer`, specifically the `StandardAnalyzer`.
     */
    open fun defaultAnalyzer(): Analyzer = StandardAnalyzer()


    /**
     * Constructs a PerFieldAnalyzerWrapper that returns different analyzers
     * for each field. Uses a StandardAnalyzer by default and KeywordAnalyzer
     * for keyword fields.
     */
    open fun analyser(): Analyzer = PerFieldAnalyzerWrapper(
        defaultAnalyzer(),
        keywordFields.associate {
            it.primary() to KeywordAnalyzer()
        }
    )


    /**
     * Tokenizes the given input string (q) based on the specified field using the schema's analyzer.
     * The method breaks down the input string into individual terms (tokens) that can be used for
     * constructing query objects. Each term is processed and collected into a list, which is returned
     * as the output.
     *
     * @param q The input string to be tokenized.
     * @param field The field to which the input string q should be mapped to.
     * @return A list of Term objects representing the tokenized form of the input string.
     */
    private fun tokenize(q: String, field: SiField): List<Term> {
        val analyzer = analyser()
        val tokenStream = analyzer.tokenStream(field.primary(), q)
        val termAttribute: TermToBytesRefAttribute = tokenStream.getAttribute(
            TermToBytesRefAttribute::class.java
        )

        val tokens = mutableListOf<Term>()
        tokenStream.reset()
        while (tokenStream.incrementToken()) {
            val term: String = termAttribute.bytesRef.utf8ToString()
            tokens.add(Term(field.primary(), term))
        }
        tokenStream.close()
        analyzer.close()
        return tokens
    }




    /**
     * Constructs a query to find a document in the search index by its unique ID.
     *
     * The query matches the provided ID against the ID field defined in the schema, enabling
     * precise retrieval of specific documents.
     *
     * @param q The ID of the document to search for.
     * @return A `TermQuery` object that represents the search query for the specified ID.
     */
    fun idQuery(q: String): Query = TermQuery(Term(idField.primary(), q))



    val defaultSort = Sort(
        SortField.FIELD_SCORE,
        if (fields.firstOrNull { it.isType(SiFieldType.SortKey) } != null) {
            SortField(SiRecord.sortField.primary(), SortField.Type.STRING)
        } else {
            SortField(idField.primary(), SortField.Type.STRING)
        }
    )


    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }


    companion object {
        val ALL: Int get() = Int.MAX_VALUE
        val logger = getAutoNamedLogger()

        val defaultIndexPath: String = "index"
    }
}
