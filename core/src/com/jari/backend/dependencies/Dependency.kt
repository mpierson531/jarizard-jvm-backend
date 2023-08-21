package com.jari.backend.dependencies

import com.jari.backend.JResult
import java.io.BufferedInputStream

abstract class Dependency {
    abstract val name: String
    abstract val version: String

    abstract fun getBytes(): ByteArray
    fun withBytes(whenDone: (ByteArray) -> Unit) = whenDone.invoke(getBytes())

    abstract fun getBufferedStream(): JResult<BufferedInputStream, java.io.IOException>
    fun withStream(withStream: (JResult<BufferedInputStream, java.io.IOException>) -> Unit) = withStream.invoke(getBufferedStream())
}