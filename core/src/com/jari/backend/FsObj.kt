package com.jari.backend

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class FsObj(pathString: String) {
    val string: String = pathString
    val path: Path = Paths.get(pathString)
    val file: File = this.path.toFile()

    override fun toString(): String = string
}