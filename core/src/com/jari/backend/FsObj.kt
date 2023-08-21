package com.jari.backend

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class FsObj @JvmOverloads constructor (val string: String, val path: Path = Paths.get(string), val file: File = path.toFile()) {
    constructor(path: Path) : this(path.toString(), path, path.toFile())

    override fun toString(): String = string

    companion object {
        inline fun getTempObj(): FsObj = FsObj(Files.createTempDirectory("jari-temp"))
    }
}