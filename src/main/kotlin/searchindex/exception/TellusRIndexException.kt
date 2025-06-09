package com.tellusr.searchindex.exception

open class TellusRIndexException(message: String, cause: Throwable? = null): Exception(message, cause) {
    class SearchIndexError(message: String, cause: Throwable? = null) : TellusRIndexException(message, cause)
    class ConstraintException(message: String, cause: Throwable? = null) : TellusRIndexException(message, cause)
    class AlreadyExistsException(message: String, cause: Throwable? = null) : TellusRIndexException(message, cause)
    class UnsupportedOperationException(message: String, cause: Throwable? = null) : TellusRIndexException(message, cause)
    class ShouldNotHappen(message: String, cause: Throwable? = null) : TellusRIndexException(message, cause)
}
