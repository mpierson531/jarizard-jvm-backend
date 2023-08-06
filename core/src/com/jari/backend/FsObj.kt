package com.jari.backend

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FsObj internal constructor(val string: String, val path: Path, val file: File) {
    constructor(pathString: String) : this(pathString, Paths.get(pathString), File(pathString))
    constructor(path: Path) : this(path.toString(), path, path.toFile())

    companion object {
        fun getTemp(): FsObj {
            val temp = Files.createTempDirectory("jari-temp")
            return FsObj(temp)
        }
    }

    override fun toString(): String = string
}