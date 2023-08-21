package com.jari.backend.dependencies

import com.jari.backend.JResult
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL

internal class MavenDependency(internal val args: Array<String>) : Dependency() {
    internal val baseUrl = "https://repo1.maven.org/maven2/"
    override val name: String = args[args.size - 2]
    override val version: String = args[args.size - 1]

    override fun getBytes(): ByteArray {
        return getFullUrl(args).readBytes()
    }

    override fun getBufferedStream(): JResult<BufferedInputStream, IOException> {
        return try {
            val stream = BufferedInputStream(getFullUrl(args).openStream())
            JResult.ok(stream)
        } catch (e: IOException) {
            JResult.err(e)
        }
    }

    private fun getFullUrl(args: Array<String>): URL {
        val lastIndex = args.size - 1
        var fullUrlString = baseUrl

        for (i in 0..lastIndex) {
            fullUrlString += "${args[i]}/"
        }

        fullUrlString += "${name}-${version}.jar"

        return URL(fullUrlString)
    }
}