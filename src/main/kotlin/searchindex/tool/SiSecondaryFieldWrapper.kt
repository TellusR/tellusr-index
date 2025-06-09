package com.tellusr.searchindex.tool

import com.tellusr.searchindex.SiField
import com.tellusr.searchindex.SiFieldType

class SiSecondaryFieldWrapper(val inner: SiField) : SiField {
    override fun fieldType(): Set<SiFieldType> = inner.fieldType()
    override fun primary(): String = inner.secondary()
    override fun secondary(): String = inner.secondary()
}