package com.jari.backend.errors

import java.io.InputStream
import java.io.OutputStream

class JarError(override val value: String) : DataError {
    override fun toString(): String = value
}