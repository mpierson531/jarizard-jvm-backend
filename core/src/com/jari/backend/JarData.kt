package com.jari.backend

import com.jari.backend.Backend.ErrorState
import com.jari.backend.dependencies.Dependency
import com.jari.backend.dependencies.MavenDep
import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.NaNError
import geo.utils.GResult
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class JarData internal constructor(input: Array<String>, output: String,
                                   dependencies: Array<Pair<String, String>>,
                                   mainClasspath: String, version: String,
                                   useCompression: Boolean) {
    companion object {
        private fun sanitize(dir: String, isDependency: Boolean): String {
            val split = splitDir(dir.trim(), isDependency)
            var i = 0

            val lastIndex = split.size - 1
            var string = ""
            val separator = if (isDependency) '.' else File.separatorChar

            if (lastIndex >= 0) {
                while (true) {
                    if (i == lastIndex) {
                        string += split[i]
                        break
                    }

                    string += split[i] + separator
                    i++
                }
            }

            return string
        }

        private fun validate(dir: String, separator: String): ErrorState {
            return if (dir.length == 1) {
                ErrorState.SingleChar
            } else if (dir.isBlank()) {
                ErrorState.Empty
            } else if (!dir.contains(separator, true)) {
                ErrorState.SeparatorNotPresent
            } else if (!Files.exists(Paths.get(dir))) {
                ErrorState.NonExistent
            } else {
                ErrorState.Ok
            }
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
    var errors: MutableList<DataError>
    val input: Array<FsObj>?
    val output: FsObj?
    val mainClasspath: FsObj?
    val version: Float?
    val useCompression: Boolean?
    val dependencies: MutableList<Dependency>

    init {
        var isOk = true
        this.errors = ArrayList()
        this.dependencies = ArrayList()

        val inputObjs = validateInput(input)

        if (inputObjs == null) {
            isOk = false
        }

        val outputObj = validateOutput(output)

        if (outputObj == null) {
            isOk = false
        }

        val mainClasspathObj = validateMainClass(mainClasspath)

        if (mainClasspathObj == null) {
            isOk = false
        }

        val trimmedJarVersion = version.trim()

        val jarVersion = if (trimmedJarVersion.isBlank()) {
            1.0f
        } else try {
            trimmedJarVersion.toFloat()
        } catch (e: NumberFormatException) {
            isOk = false
            errors.add(NaNError(version, true))
            1.0f
        }

        if (!validateDependencies(dependencies)) {
            isOk = false
        }

        this.version = jarVersion
        this.useCompression = useCompression

        if (isOk) {
            this.isOk = true
            this.input = Array(inputObjs!!.size) { inputObjs[it] }
            this.output = outputObj
            this.mainClasspath = mainClasspathObj
        } else {
            this.isOk = false
            this.input = null
            this.output = null
            this.mainClasspath = null
        }
    }

    private fun validateInput(input: Array<String>): Array<FsObj>? {
        val inputObjs: Array<GResult<FsObj, DataError>> = Array(input.size) {
            try {
                val sanitized = sanitize(input[it], false)
                GResult.ok(FsObj(sanitized))
            } catch (e: java.lang.Exception) {
                GResult.err(IOError(input[it], ErrorState.Exception, "Input Directory"))
            }
        }

        var isOk = true

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

        return if (isOk) Array(inputObjs.size) { inputObjs[it].unwrap() } else null
    }

    private fun validateOutput(output: String): FsObj? {
        val outputResult: GResult<FsObj, DataError> = try {
            GResult.ok(FsObj(sanitize(output, false)))
        } catch (e: java.lang.Exception) {
            GResult.err(IOError(output, ErrorState.Exception, "Output Directory"))
        }

        return if (outputResult.isOk) {
            val unwrapped = outputResult.unwrap()
            val outValidation = validate(unwrapped)

            if (outValidation != ErrorState.Ok && outValidation != ErrorState.NonExistent) {
                errors.add(IOError(unwrapped.string, outValidation, "Output Directory"))
                null
            } else {
                unwrapped
            }
        } else {
            errors.add(outputResult.unwrapErr())
            null
        }
    }

    private fun validateMainClass(mainClasspath: String): FsObj? {
        val mainClasspathChars = sanitize(mainClasspath.replace('.', File.separatorChar), false).toCharArray()

        var mainClasspathString = ""
        var i = 0

        while (i < mainClasspathChars.size) {
            if (mainClasspathChars[i] == File.separatorChar) {
                var string = ""
                var j = i + 1

                while (j < mainClasspathChars.size && j < i + 6) {
                    string += mainClasspathChars[j]
                    j++
                }

                if (string == "class") {
                    i += 6
                    continue
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

        return if (mainClasspath.isOk) {
            val unwrapped = mainClasspath.unwrap()
            val classValidation = validate(unwrapped)

            if (classValidation == ErrorState.SingleChar || classValidation == ErrorState.SeparatorNotPresent) {
                errors.add(IOError(unwrapped.string, classValidation, "Main Class-Path"))
                null
            } else {
                unwrapped
            }
        } else {
            null
        }
    }

    private fun validateDependencies(dependencies: Array<Pair<String, String>>): Boolean {
        var isOk = true

        for (dependency in dependencies) {
            val trimmedDepPath = dependency.first.trim()
            val splitDepPath = splitDir(trimmedDepPath, true)
            val lastIndex = splitDepPath.size - 1
            var sanitizedDepPath = ""

            for (k in 0..lastIndex) {
                sanitizedDepPath += if (k == lastIndex) splitDepPath[k] else splitDepPath[k] + '.'
            }

            println("Sanitized Dep: $sanitizedDepPath")

            val pathValidation = validate(sanitizedDepPath, ".")

            val trimmedDepVersion = dependency.second.trim()
            val versionValidation = validate(trimmedDepVersion, ".")

            if (pathValidation != ErrorState.Ok && pathValidation != ErrorState.NonExistent) {
                isOk = false
                errors.add(IOError(dependency.first, pathValidation, "Dependency"))
            }

            if (versionValidation != ErrorState.Ok && versionValidation != ErrorState.NonExistent) {
                isOk = false
                errors.add(IOError(dependency.second, versionValidation, "Dependency Version"))
                continue
            }

            val path = splitDir(sanitizedDepPath, true) + trimmedDepVersion
            this.dependencies.add(MavenDep(path))
        }

        return isOk
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