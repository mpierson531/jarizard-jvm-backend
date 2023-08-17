package com.jari.backend

import com.jari.backend.dependencies.Dependency
import com.jari.backend.dependencies.DependencyHandler
import com.jari.backend.dsl.DSLParser
import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.JarError
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.io.path.name

internal fun splitDir(dir: String, isDependency: Boolean): Array<String> {
    var i = 0
    val separator = if (isDependency) '.' else File.separatorChar
    val split = ArrayList<String>()

    while (i < dir.length) {
        if (dir[i] != separator) {
            var string = ""

            while (i < dir.length && dir[i] != separator) {
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

    private var isOkAtomic: AtomicBoolean = AtomicBoolean(false)
    private var isRunningAtomic: AtomicBoolean = AtomicBoolean(false)
    private var jarDataAtomic: AtomicReference<MutableList<JarData>> = AtomicReference(ArrayList())
    private var errorsAtomic: AtomicReference<MutableList<DataError>> = AtomicReference(ArrayList())

    val isDone: Boolean get() = !isRunningAtomic.get()
    val isOk: Boolean get() = isOkAtomic.get()

    val errors: MutableList<DataError> get() = errorsAtomic.get()

    private companion object {
        private fun invokeJar(startDirectory: File, input: MutableList<String>, output: String, useCompression: Boolean): JResult<Process, DataError> {
            // TODO: INTRODUCE MORE ERROR HANDLING HERE

            val jar = try {
                "${System.getenv("JAVA_HOME")}${File.separatorChar}bin${File.separatorChar}jar.exe"
            } catch (e: java.lang.Exception) {
                return JResult.err(IOError("JAVA_HOME", ErrorState.Exception, "unable to find jar executable within JAVA_HOME"))
            }

            val list = ArrayList<String>(input.size + 4)

            if (!useCompression) {
                list.addAll(arrayOf(jar, "cfm0", "\"$output\"", "MANIFEST.txt"))
            } else {
                list.addAll(arrayOf(jar, "cfm", "\"$output\"", "MANIFEST.txt"))
            }

            list.addAll(Array(input.size) { "\"${input[it]}\"" })

            return try {
                JResult.ok(ProcessBuilder(list).redirectErrorStream(true).directory(startDirectory).start())
            } catch (e: java.lang.Exception) {
                JResult.err(IOError(e.message, ErrorState.Exception, "Jar"))
            }
        }

        private fun writeManifest(dir: String, mainClass: String?, version: Float?) {
            val manifestContent = if (mainClass == null) {
                "Manifest-Version: ${version ?: 1.0f.toString()}${System.lineSeparator()}"
            } else {
                "Manifest-Version: ${version ?: 1.0f.toString()}${System.lineSeparator()}Main-Class: $mainClass${System.lineSeparator()}"
            }

            Files.write(Paths.get("${dir}MANIFEST.txt"), manifestContent.toByteArray())
        }

        private fun recursivelyDelete(dir: Path?) {
            try {
                if (Files.isDirectory(dir)) {
                    Files.newDirectoryStream(dir).use { stream ->
                        for (fsObj in stream) {
                            recursivelyDelete(fsObj)
                        }
                    }
                }
                Files.delete(dir)
            } catch (e: IOException) {
                println(e.message)
            }
        }
    }

    fun jarIt(file: File) {
        val jarDataResult = DSLParser.parse(file)

        if (jarDataResult.isOk) {
            jarIt(jarDataResult.unwrap())
        } else {
            errors.addAll(jarDataResult.unwrapErr())
        }
    }

    fun jarIt(input: Array<String>, output: String, dependencies: Array<Pair<String, String>>, mainClass: String, version: String, useCompression: Boolean) {
        jarIt(JarData(input, output, dependencies, mainClass, version, useCompression))
    }

    private fun jarIt(jarData: JarData) {
        val isJarDataSuccess = validateJarData(jarData)

        println(isOkAtomic.get())

        println("before check success")

        if (isRunningAtomic.get()) {
            return
        }

        if (isJarDataSuccess) {
            setIsOk(true)
            setRunning(true)

            when (Runtime.getRuntime().availableProcessors()) {
                0 -> jarIt(false)
                1 -> thread { jarIt(false) }
                else -> thread { jarIt(true) }
            }
        }
    }

    private fun jarIt(useDependencyThread: Boolean) {
        while (jarDataAtomic.get().size > 0) {
            val temp = FsObj.getTempObj()
            val tempRoot = temp.string + File.separatorChar
            val jarData = jarDataAtomic.get().removeAt(0)
            val jarArgs = AtomicReference(mutableListOf<String>())
            val isDependencyOk = AtomicBoolean(true)

            val dependencyThread =
                handleDependencies(useDependencyThread, tempRoot, jarData.dependencies, jarArgs, isDependencyOk)

            copyAndAddInput(tempRoot, jarData.input!!, jarArgs)

            dependencyThread?.join()

            if (!isDependencyOk.get()) {
                setIsOk(false)
            }

            if (isOk) {
                val mainClassString = validateMainClass(tempRoot, jarData.mainClasspath)
                writeManifest(tempRoot, mainClassString?.replace(File.separatorChar, '.'), jarData.version)
                handleJar(temp.file, jarArgs.get(), jarData.output!!.string, jarData.useCompression)
            }

            recursivelyDelete(temp.path)
        }

        setRunning(false)
    }

    private fun handleDependencies(withNewThread: Boolean,
                                   rootDir: String, dependencies: MutableList<Dependency>,
                                   jarArgs: AtomicReference<MutableList<String>>, isOk: AtomicBoolean): Thread? {
        if (dependencies.isEmpty()) {
            return null
        }

        return if (withNewThread) {
            thread { downloadDependencies(rootDir, dependencies, jarArgs, isOk) }
        } else {
            downloadDependencies(rootDir, dependencies, jarArgs, isOk)
            null
        }
    }

    private fun downloadDependencies(rootDir: String, dependencies: MutableList<Dependency>,
                                     jarArgs: AtomicReference<MutableList<String>>, isOk: AtomicBoolean) {
        val dependencyHandler = DependencyHandler()

        dependencyHandler.download(rootDir, dependencies) {
            jarArgs.get().add("${it.name}-${it.version}.jar")
        }

        if (!dependencyHandler.isOk) {
            isOk.set(false)
            errorsAtomic.get().addAll(dependencyHandler.errors)
        } else {
            isOk.set(true)
        }
    }

    private fun copyAndAddInput(rootDir: String, input: Array<FsObj>, jarArgs: AtomicReference<MutableList<String>>) {
        for (inp in input) {
            inp.file.copyRecursively(File(rootDir + inp.path.name), true) { _, exception ->
                if (isOk) {
                    setIsOk(false)
                }

                addError(IOError(exception.message, ErrorState.Exception, "Error"))
                OnErrorAction.SKIP
            }

            jarArgs.get().add(inp.path.name)
        }
    }

    private fun validateMainClass(rootDir: String, mainClass: FsObj?): String? {
        val mainClass = if (mainClass != null && mainClass.string.isBlank()) {
            null
        } else {
            mainClass
        }

        if (mainClass != null) {
            val fullClasspath = if (mainClass.path.isAbsolute) {
                Paths.get("${mainClass.string}.class")
            } else {
                Paths.get("$rootDir${mainClass.string}.class")
            }

            if (!Files.exists(fullClasspath)) {
                addError(IOError("\"${fullClasspath}\"", ErrorState.NonExistent, "Main Class-Path"))
                setIsOk(false)
            }
        }

        return mainClass?.string
    }

    private fun handleJar(startDirectory: File, args: MutableList<String>, output: String, useCompression: Boolean) {
        val process = invokeJar(startDirectory, args, output, useCompression)

        if (process.isOk) {
            val unwrappedProcess = process.unwrap()
            unwrappedProcess.waitFor()

            if (unwrappedProcess.exitValue() != 0) {
                setIsOk(false)
                addError(JarError(String(unwrappedProcess.inputStream.readAllBytes())))
            }
        } else {
            setIsOk(false)
            addError(process.unwrapErr())
        }
    }

    private fun validateJarData(jarData: JarData): Boolean {
        if (jarData.isOk) {
            println("jar data is ok")
            jarDataAtomic.get().add(jarData)
            return true
        } else {
            println("jar data is not ok")
            setIsOk(false)
            errorsAtomic.get().addAll(jarData.errors)
            return false
        }
    }

    private fun addError(error: DataError) = errorsAtomic.get().add(error)

    private fun setRunning(isRunning: Boolean) = isRunningAtomic.set(isRunning)
    private fun setIsOk(isOk: Boolean) = isOkAtomic.set(isOk)

    fun reset() {
        errorsAtomic.get().clear()
        jarDataAtomic.get().clear()
        isOkAtomic.set(false)
    }
}