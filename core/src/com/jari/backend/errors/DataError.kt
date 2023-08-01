package com.jari.backend.errors

interface DataError {
    val value: String
    override fun toString(): String
}