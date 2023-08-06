package com.jari.backend

import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.JarError
import geo.files.FileHandler
import geo.threading.ThreadUtils
import geo.utils.GResult
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.name

fun splitDir(dir: String, isDependency: Boolean): Array<String> {
    var i = 0
    val separator = if (isDependency) '.' else File.separatorChar
    val split = ArrayList<String>()

    while (i < dir.length) {
        if (dir[i] != separator) {
            var string = ""

            while (i != dir.length && dir[i] != separator) {
                string += dir[i]
                i++
            }

            split.add(string)
        }

        i++
    }

    return split.toTypedArray()
}

class Backend {
    enum class ErrorState {
        Empty,
        SingleChar,
        SeparatorNotPresent,
        NonExistent,
        Exception,
        Ok;

        override fun toString(): String {
            return when (this) {
                Ok -> "Input Jarred!"
                NonExistent -> "non-existent"
                SeparatorNotPresent -> "no file separator present"
                SingleChar -> "single-character"
                Empty -> "was empty"
                Exception -> "unable to get directory"
            }
        }
    }

    private var isDoneAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var isOkAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var isRunningAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var jarDataAtomic: AtomicReference<JarData> = AtomicReference()
    private var errorsAtomic: AtomicReference<MutableList<DataError>> = AtomicReference(ArrayList())

    val isDone: Boolean get() = isDoneAtomic.get()
    val isOk: Boolean get() = isOkAtomic.get() && jarDataAtomic.get().isOk
    val errors: MutableList<DataError> get() = errorsAtomic.get()

    companion object {
        private inline fun invokeJar(startDirectory: File, input: Array<String>, output: String, useCompression: Boolean): GResult<Process, DataError> {
            // TODO: INTRODUCE MORE ERROR HANDLING HERE

            val jar = try {
                "${System.getenv("JAVA_HOME")}${File.separator}bin${File.separator}jar.exe"
            } catch (e: java.lang.Exception) {
                return GResult.err(IOError("JAVA_HOME", ErrorState.Exception, "unable to find jar executable"))
            }

            val list = ArrayList<String>(input.size + 4)

            if (!useCompression) {
                list.addAll(arrayOf(jar, "cfm0", "\"$output\"", "MANIFEST.txt"))
            } else {
                list.addAll(arrayOf(jar, "cfm", "\"$output\"", "MANIFEST.txt"))
            }

            list.addAll(Array(input.size) { "\"${input[it]}\"" })

            val processBuilder = ProcessBuilder(list).inheritIO().directory(startDirectory)

            return try {
                GResult.ok(processBuilder.start())
            } catch (e: java.lang.Exception) {
                GResult.err(IOError(e.message, ErrorState.Exception, "Jar"))
            }
        }

        private inline fun writeManifest(dir: String, mainClass: String?, version: Float?) {
            var fullText = "Manifest-Version: ${version ?: 1.0f.toString()}${System.lineSeparator()}"

            if (mainClass != null) {
                fullText += "Main-Class: $mainClass${System.lineSeparator()}"
            }

            Files.write(Paths.get("$dir${File.separator}MANIFEST.txt"), fullText.toByteArray())
        }

        private inline fun cleanUp(dir: Path) = FileHandler.recursivelyDelete(dir)
    }

    fun jarIt(input: Array<String>, output: String, depedencies: Array<Pair<String, String>>, mainClass: String, version: String, useCompression: Boolean) {
        if (isRunningAtomic.get()) {
            return
        }

        val jarData = JarData(input, output, depedencies, mainClass, version, useCompression)

        if (jarData.isOk) {
            jarDataAtomic.set(jarData)
            jarIt()
        } else {
            errorsAtomic.get().addAll(jarData.errors)
            isOkAtomic.set(false)
            setNotRunning()
        }
    }

    private fun jarIt() {
        setRunning()

        ThreadUtils.run {
            val temp = FsObj.getTemp()
            val jarData = jarDataAtomic.get()
            val tempRoot = temp.string + File.separator
            var isOk = true

            val inputArgs = mutableListOf<String>()

            for (inp in jarData.input!!) {
                inp.file.copyRecursively(File(tempRoot + inp.path.name), true) { _, exception -> throw exception }
                inputArgs.add(inp.path.name)
            }

            val mainClass = if (jarData.mainClasspath != null && jarData.mainClasspath.string.isBlank()) {
                null
            } else {
                jarData.mainClasspath
            }

            if (mainClass != null) {
                val fullClasspath = if (mainClass.path.isAbsolute) {
                    Paths.get(mainClass.string + ".class")
                } else {
                    Paths.get(temp.string + File.separator + mainClass + ".class")
                }

                if (!Files.exists(fullClasspath)) {
                    errorsAtomic.get().add(IOError("\"${fullClasspath}\"", ErrorState.NonExistent, "Main Class-Path"))
                    cleanUp(temp.path)
                    isOk = false
                }
            }

            for (dependency in jarData.dependencies) {
                try {
                    val result = dependency.getStream()

                    if (result.isOk) {
                        val fileObj = FsObj("${temp.string}${File.separator}${dependency.name}-${dependency.version}.jar")

                        println("Dependency Name: ${dependency.name}")
                        println("FsObj Name: ${fileObj.path.name}")

                        fileObj.file.createNewFile()
                        fileObj.file.writeBytes(dependency.getBytes())
                        inputArgs.add(fileObj.path.name)
                    } else {
                        isOk = false
                        errorsAtomic.get().add(IOError(result.unwrapErr().toString(), ErrorState.Exception, "Error"))
                    }
                } catch (e: java.lang.Exception) {
                    isOk = false
                    errorsAtomic.get().add(IOError(e.message, ErrorState.Exception, "Error"))
                }
            }

            if (isOk) {
                writeManifest(temp.string, mainClass?.string?.replace(File.separatorChar, '.'), jarData.version)
                val process =
                    invokeJar(temp.file, inputArgs.toTypedArray(), jarData.output!!.string, jarData.useCompression!!)

                if (process.isOk) {
                    val unwrappedProcess = process.unwrap()
                    unwrappedProcess.waitFor()

                    if (unwrappedProcess.exitValue() != 0) {
                        var error = ""

                        val reader = unwrappedProcess.errorStream.bufferedReader()
                        var line = reader.readLine()

                        while (line != null) {
                            error += line
                            line = reader.readLine()
                        }

                        jarData.errors.add(JarError(error))
                        isOk = false
                    }
                } else {
                    errorsAtomic.get().add(process.unwrapErr())
                    isOk = false
                }
            }

            cleanUp(temp.path)
            isOkAtomic.set(isOk)
            setNotRunning()
        }
    }

    private fun setRunning() {
        isDoneAtomic.set(false)
        isRunningAtomic.set(true)
    }

    private fun setNotRunning() {
        isRunningAtomic.set(false)
        isDoneAtomic.set(true)
    }

    fun clear() {
        jarDataAtomic.set(null)
        isOkAtomic.set(false)
    }
}