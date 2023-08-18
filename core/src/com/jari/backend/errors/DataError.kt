package com.jari.backend.errors

abstract class DataError {
    abstract val value: String
    override fun toString(): String = value
}