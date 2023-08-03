package com.jari.backend

import com.jari.backend.Backend.ErrorState
import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.NaNError
import geo.collections.FysList
import geo.utils.GResult
import java.io.File
import java.nio.file.Files

class JarData internal constructor(input: Array<String>, output: String,
                                   mainClasspath: String, version: String,
                                   useCompression: Boolean) {
    companion object {
        private fun sanitize(dir: String): String {
            val split = splitDir(dir)
            var i = 0

            val lastIndex = split.size - 1
            var string = ""

            if (lastIndex >= 0) {
                while (true) {
                    if (i == lastIndex) {
                        string += split[i]
                        break
                    }

                    string += split[i] + File.separator
                    i++
                }
            }

            return string
        }

        private fun validate(dir: FsObj): ErrorState {
            return if (dir.string.length == 1) {
                ErrorState.SingleChar
            } else if (dir.string.isBlank()) {
                ErrorState.Empty
            } else if (!dir.string.contains(File.separator, true)) {
                ErrorState.SeparatorNotPresent
            } else if (!Files.exists(dir.path)) {
                ErrorState.NonExistent
            } else {
                ErrorState.Ok
            }
        }
    }

    val isOk: Boolean
    var errors: FysList<DataError>
    val input: Array<FsObj>?
    val output: FsObj?
    val mainClasspath: FsObj?
    val version: Float?
    val useCompression: Boolean?

    init {
        var isOk = true
        this.errors = FysList()

        val inputObjs: Array<GResult<FsObj, DataError>> = Array(input.size) {
            try {
                val sanitized = sanitize(input[it])
                GResult.ok(FsObj(sanitized))
            } catch (e: java.lang.Exception) {
                GResult.err(IOError(input[it], ErrorState.Exception, "Input Directory"))
            }
        }

        for (i in 0..input.size - 1) {
            if (inputObjs[i].isOk) {
                val validation = validate(inputObjs[i].unwrap())

                if (validation != ErrorState.Ok) {
                    isOk = false
                    errors.add(IOError(input[i], validation, "Input Directory"))
                }
            } else {
                isOk = false
                errors.add(inputObjs[i].unwrapErr())
            }
        }

        val output: GResult<FsObj, DataError> = try {
            GResult.ok(FsObj(sanitize(output)))
        } catch (e: java.lang.Exception) {
            isOk = false
            GResult.err(IOError(output, ErrorState.Exception, "Output Directory"))
        }

        if (output.isOk) {
            val unwrapped = output.unwrap()
            val outValidation = validate(unwrapped)

            if (outValidation != ErrorState.Ok && outValidation != ErrorState.NonExistent) {
                isOk = false
                errors.add(IOError(unwrapped.string, outValidation, "Output Directory"))
            }
        }

        val mainClasspathChars = sanitize(mainClasspath).toCharArray()

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

        val mainClasspath: GResult<FsObj, DataError> = try {
            GResult.ok(FsObj(mainClasspathString))
        } catch (e: java.lang.Exception) {
            GResult.err(IOError(mainClasspath, ErrorState.Exception, "Main Class-Path"))
        }

        if (mainClasspath.isOk) {
            val unwrapped = mainClasspath.unwrap()
            val classValidation = validate(unwrapped)

            if (classValidation == ErrorState.SingleChar || classValidation == ErrorState.SeparatorNotPresent) {
                isOk = false
                errors.add(IOError(unwrapped.string, classValidation, "Main Class-Path"))
            }
        }

        val trimmedVersion = version.trim()

        val version = if (trimmedVersion.isBlank()) {
            1.0f
        } else try {
            trimmedVersion.toFloat()
        } catch (e: NumberFormatException) {
            isOk = false
            errors.add(NaNError(version, true))
            1.0f
        }

        this.version = version
        this.useCompression = useCompression

        if (isOk) {
            this.isOk = true
            this.input = Array(inputObjs.size) { inputObjs[it].unwrap() }
            this.output = output.unwrap()
            this.mainClasspath = mainClasspath.unwrap()
        } else {
            this.isOk = false
            this.input = null
            this.output = null
            this.mainClasspath = null
        }
    }

    override fun toString(): String {
        val string = if (input != null) {
            if (input.isEmpty()) {
                "empty${System.lineSeparator()}"
            } else {
                val lastIndex = input.size - 1
                var string = ""

                for (i in 0..lastIndex) {
                    string += input[i].toString() + System.lineSeparator()
                }

                string
            }
        } else {
            "null${System.lineSeparator()}"
        }

        return string +
                "output: $output${System.lineSeparator()}" +
                "mainClasspath: $mainClasspath${System.lineSeparator()}" +
                "version: $version${System.lineSeparator()}" +
                "compression: $useCompression"
    }
}