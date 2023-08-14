package com.jari.backend

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class FsObj(val string: String, val path: Path, val file: File) {
    constructor(pathString: String) : this(pathString, Paths.get(pathString), File(pathString))
    constructor(path: Path) : this(path.toString(), path, path.toFile())

    override fun toString(): String = string

    companion object {
        inline fun getTempObj(): FsObj = FsObj(Files.createTempDirectory("jari-temp"))
    }
}