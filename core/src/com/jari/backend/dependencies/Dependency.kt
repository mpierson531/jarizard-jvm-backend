package com.jari.backend.dependencies

import geo.utils.GResult
import java.io.BufferedInputStream

abstract class Dependency {
    abstract val name: String
    abstract val version: String

    abstract fun getBytes(): ByteArray
    abstract fun withBytes(whenDone: (ByteArray) -> Unit)

    abstract fun getStream(): GResult<BufferedInputStream, java.io.IOException>
    abstract fun withStream(withStream: (GResult<BufferedInputStream, java.io.IOException>) -> Unit)
}