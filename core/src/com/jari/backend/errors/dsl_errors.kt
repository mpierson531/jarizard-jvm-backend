package com.jari.backend.errors

open class BaseDSLError(val message: String, val lineNumber: Int) : DataError() {
    override val value: String = "$message line $lineNumber"
}

open class BaseUnexpectedValueErr(foundValue: String, lineNumber: Int, suffix: String)
    : BaseDSLError("Unexpected $foundValue$suffix", lineNumber)

open class ExpectedValueErr(foundValue: String, lineNumber: Int, expected: String)
    : BaseUnexpectedValueErr(foundValue, lineNumber, ", expected $expected,")