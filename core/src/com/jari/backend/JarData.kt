package com.jari.backend

import com.jari.backend.Backend.ErrorState
import com.jari.backend.dependencies.Dependency
import com.jari.backend.dependencies.MavenDependency
import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.NaNError
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class JarData internal constructor(input: Array<String>, output: String,
                       dependencies: Array<Pair<String, String>>,
                       mainClasspath: String, version: String,
                       useCompression: Boolean) {
    companion object {
        private fun sanitize(dir: String): String {
            val split = splitDir(dir.trim(), false)
            var i = 0

            val lastIndex = split.size - 1
            var string = ""
            val separator = File.separatorChar

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

        private fun validateWithDot(dir: String): ErrorState {
            return if (dir.length == 1) {
                ErrorState.SingleChar
            } else if (dir.isBlank()) {
                ErrorState.Empty
            } else if (!dir.contains('.', true)) {
                ErrorState.SeparatorNotPresent
            } else {
                ErrorState.Ok
            }
        }

        private fun validateWithFile(dir: FsObj): ErrorState = validateDirectory(dir.string, dir.path)

        private fun validateDirectory(dir: String, path: Path): ErrorState {
            return if (dir.length == 1) {
                ErrorState.SingleChar
            } else if (dir.isBlank()) {
                ErrorState.Empty
            } else if (!dir.contains(File.separatorChar, true)) {
                ErrorState.SeparatorNotPresent
            } else if (!Files.exists(path)) {
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
    val useCompression: Boolean
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

        val jarVersion = getJarVersion(version)

        if (jarVersion == null) {
            isOk = false
        }

        if (!validateDependencies(dependencies)) {
            isOk = false
        }

        this.isOk = isOk
        this.input = inputObjs
        this.output = outputObj
        this.mainClasspath = mainClasspathObj
        this.version = jarVersion
        this.useCompression = useCompression
    }

    private fun validateInput(input: Array<String>): Array<FsObj>? {
        val inputObjs: Array<JResult<FsObj, DataError>> = Array(input.size) {
            try {
                val sanitized = sanitize(input[it])
                JResult.ok(FsObj(sanitized))
            } catch (e: java.lang.Exception) {
                JResult.err(IOError(input[it], ErrorState.Exception, "Input Directory"))
            }
        }

        var isOk = true

        for (i in input.indices) {
            if (inputObjs[i].isOk) {
                val validation = validateWithFile(inputObjs[i].unwrap())

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
        val outputResult: JResult<FsObj, DataError> = try {
            JResult.ok(FsObj(sanitize(output)))
        } catch (e: java.lang.Exception) {
            JResult.err(IOError(output, ErrorState.Exception, "Output Directory"))
        }

        return if (outputResult.isOk) {
            val unwrapped = outputResult.unwrap()
            val outValidation = validateWithFile(unwrapped)

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
        val mainClasspathChars = sanitize(mainClasspath.replace('.', File.separatorChar)).toCharArray()

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

        if (mainClasspathString.isBlank()) {
            return FsObj("")
        }

        val mainClasspath: JResult<FsObj, DataError> = try {
            JResult.ok(FsObj(mainClasspathString))
        } catch (e: java.lang.Exception) {
            JResult.err(IOError(mainClasspath, ErrorState.Exception, "Main Class-Path"))
        }

        return if (mainClasspath.isOk) {
            val unwrapped = mainClasspath.unwrap()
            val classValidation = validateWithFile(unwrapped)

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

            val pathValidation = validateWithDot(sanitizedDepPath)

            val trimmedDepVersion = dependency.second.trim()
            val versionValidation = validateWithDot(trimmedDepVersion)

            if (pathValidation != ErrorState.Ok) {
                isOk = false
                errors.add(IOError(dependency.first, pathValidation, "Dependency"))
            }

            if (versionValidation != ErrorState.Ok) {
                isOk = false
                errors.add(IOError(dependency.second, versionValidation, "Dependency Version"))
                continue
            }

            val path = splitDir(sanitizedDepPath, true) + trimmedDepVersion
            this.dependencies.add(MavenDependency(path))
        }

        return isOk
    }

    private fun getJarVersion(version: String): Float? {
        val trimmedVersion = version.trim()

        return if (trimmedVersion.isBlank()) {
            1.0f
        } else try {
            trimmedVersion.toFloat()
        } catch (e: NumberFormatException) {
            errors.add(NaNError(version, true))
            null
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