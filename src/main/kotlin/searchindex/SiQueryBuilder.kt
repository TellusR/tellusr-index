package com.tellusr.searchindex

import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query

/**
 * Builder interface for constructing Lucene search queries.
 * Implementations of this interface provide different ways to build
 * query objects that can be used for searching in a Lucene index.
 */
interface SiQueryBuilder {
    /**
     * Builds and returns a Lucene Query object based on the configured criteria.
     *
     * @return A Query object that can be used for searching in a Lucene index.
     */
    fun build(): Query


    companion object {
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
        fun tokenize(schema: SiSchema, field: SiField, q: String): List<Term> {
            val analyzer = schema.analyser()
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
    }
}