package com.jari.backend.dependencies

import geo.utils.GResult
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL

class MavenDep(internal val args: Array<String>) : Dependency() {
    internal val baseUrl = "https://repo1.maven.org/maven2/"
    override val name: String = args[args.size - 2]
    override val version: String = args[args.size - 1]

    override fun getBytes(): ByteArray {
        return getFullUrl(args).readBytes()
    }

    override fun withBytes(whenDone: (ByteArray) -> Unit) {
        whenDone.invoke(getBytes())
    }

    override fun getStream(): GResult<BufferedInputStream, IOException> {
        return try {
            val stream = BufferedInputStream(getFullUrl(args).openStream())
            GResult.ok(stream)
        } catch (e: IOException) {
            GResult.err(e)
        }
    }

    override fun withStream(withStream: (GResult<BufferedInputStream, IOException>) -> Unit) {
        withStream.invoke(getStream())
    }

    private fun getFullUrl(args: Array<String>): URL {
        val lastIndex = args.size - 1
        var fullUrlString = baseUrl

        for (i in 0..lastIndex) {
//            fullUrlString += if (i == lastIndex) args[i] else "${args[i]}/"
            fullUrlString += "${args[i]}/"
        }

        fullUrlString += "${name}-${version}.jar"

        println(fullUrlString)

        return URL(fullUrlString)
    }
}