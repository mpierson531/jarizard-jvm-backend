package com.jari.backend

import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.NaNError
import geo.collections.FysList
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class JarData internal constructor(input: Array<String>, output: String,
                                   mainClasspath: String, version: String,
                                   useCompression: Boolean) {
    companion object {
        private inline fun sanitize(dir: String): String {
            val dir = dir.trim().split(File.separator).toMutableList()

            var string = ""

            if (dir[dir.size - 1] == File.separator) {
                while (dir[dir.size - 1] == File.separator) {
                    dir.removeAt(dir.size - 1)
                }
            }

            for (i in 0..dir.size - 1) {
                if (i == dir.size - 1) {
                    string += dir[i]
                    break
                }

                string += dir[i] + File.separator
            }

            return string
        }

        private inline fun validate(dir: String): ErrorState {
            return if (dir.length == 1) {
                ErrorState.SingleChar
            } else if (dir.isBlank()) {
                ErrorState.Empty
            } else if (!dir.contains(File.separator, true)) {
                ErrorState.SeparatorNotPresent
            } else if (!Files.exists(Paths.get(dir))) {
                ErrorState.NonExistent
            } else {
                ErrorState.Ok
            }
        }
    }

    enum class ErrorState {
        Empty,
        SingleChar,
        SeparatorNotPresent,
        NonExistent,
        Ok;

        override fun toString(): String {
            return when (this) {
                Ok -> "Input Jarred!"
                NonExistent -> "non-existent"
                SeparatorNotPresent -> "no file separator present"
                SingleChar -> "single-character"
                Empty -> "was empty"
            }
        }
    }

    val isOK: Boolean
    var errors: FysList<DataError>
    val input: Array<String>?
    val output: String?
    val mainClasspath: String?
    val version: Float?
    val useCompression: Boolean?

    init {
        var isOk = true
        val errors = FysList<DataError>()

        for (i in 0..input.size - 1) {
            input[i] = sanitize(input[i])
            val validation = validate(input[i])

            if (validation != ErrorState.Ok) {
                isOk = false
                errors.add(IOError(input[i], validation, "Input Directory"))
            }
        }

        val output = sanitize(output)
        val outValidation = validate(output)

        if (outValidation != ErrorState.Ok && outValidation != ErrorState.NonExistent) {
            isOk = false
            errors.add(IOError(output, outValidation, "Output Directory"))
        }

        val mainClasspathChars = mainClasspath.toMutableList()

        if (mainClasspathChars.isNotEmpty()) {
            while (mainClasspathChars.isNotEmpty() && mainClasspathChars[0] == File.separatorChar || mainClasspathChars[0] == '.') {
                mainClasspathChars.removeAt(0)
            }
        }

        var mainClasspathString = ""
        var i = 0

        while (i < mainClasspathChars.size) {
            if (mainClasspathChars[i] == '.') {
                var string = ""
                var j = i + 1

                while (j < mainClasspathChars.size && j < i + 6) {
                    string += mainClasspathChars[j]
                    j++
                }

                if (string == "class") {
                    i += 6
                    continue
                } else {
                    mainClasspathChars[i] = File.separatorChar
                }
            }

            mainClasspathString += mainClasspathChars[i]
            i++
        }

        var mainClasspath: String? = sanitize(mainClasspathString)

        val classValidation = validate(mainClasspath!!)

        if (classValidation == ErrorState.SingleChar || classValidation == ErrorState.SeparatorNotPresent) {
            isOk = false
            errors.add(IOError(mainClasspath, classValidation, "Main Class-Path"))
            mainClasspath = null
        }

        val trimmedVersion = version.trim()

        val version = if (trimmedVersion.isBlank()) {
            1.0f
        } else try {
            trimmedVersion.toFloat()
        } catch (e: NumberFormatException) {
            isOk = false
            errors.add(NaNError(trimmedVersion, true))
            null
        }

        if (!isOk) {
            this.isOK = false
            this.errors = errors
            this.input = null
            this.output = null
            this.mainClasspath = null
            this.version = null
            this.useCompression = null
        } else {
            this.isOK = true
            this.errors = FysList()
            this.input = input
            this.output = output
            this.mainClasspath = mainClasspath
            this.version = version
            this.useCompression = useCompression
        }
    }

    override fun toString(): String {
        var string = "input: ${System.lineSeparator()}"

        if (input != null) {
            val lastIndex = input.size - 1

            for (i in 0..lastIndex) {
                string += if (i == lastIndex) {
                    "${input[i]}${System.lineSeparator()}"
                } else {
                    "${input[i]}, "
                }
            }
        } else {
            string += "null${System.lineSeparator()}"
        }

        return string +
                "output: $output${System.lineSeparator()}" +
                "mainClasspath: $mainClasspath${System.lineSeparator()}" +
                "version: $version${System.lineSeparator()}" +
                "compression: $useCompression"
    }
}