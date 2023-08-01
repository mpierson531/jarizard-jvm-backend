package com.jari.backend

import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import com.jari.backend.errors.JarError
import geo.Fys
import geo.collections.FysList
import geo.files.FileHandler
import geo.threading.ThreadUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.name

class Backend {
    private var isDoneAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var isOkAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var isRunningAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var jarDataAtomic: AtomicReference<JarData> = AtomicReference()

    val isDone: Boolean get() = isDoneAtomic.get()
    val isOk: Boolean get() = isOkAtomic.get() && jarDataAtomic.get().isOK
    val errors: FysList<DataError> get() = jarDataAtomic.get().errors

    companion object {
        private inline fun invokeJar(startDirectory: File, input: Array<String>, output: String, useCompression: Boolean): Process {
            // TODO: INTRODUCE MORE ERROR HANDLING HERE

            val jar = "${System.getenv("JAVA_HOME")}${File.separator}bin${File.separator}jar.exe"

            val list = ArrayList<String>(input.size + 4)

            if (!useCompression) {
                list.addAll(arrayOf(jar, "cfm0", "\"$output\"", "MANIFEST.txt"))
            } else {
                list.addAll(arrayOf(jar, "cfm", "\"$output\"", "MANIFEST.txt"))
            }

            list.addAll(Array(input.size) { "\"${input[it]}\"" })

            return ProcessBuilder(list)
                .inheritIO()
                .directory(startDirectory)
                .start()
        }

        private inline fun writeManifest(dir: String, mainClass: String?, version: Float?) {
            var fullText = "Manifest-Version: ${version ?: 1.0f.toString()}${System.lineSeparator()}"

            if (mainClass != null) {
                fullText += "Main-Class: $mainClass${System.lineSeparator()}"
            }

            Files.write(Paths.get("$dir${File.separator}MANIFEST.txt"), fullText.toByteArray())

            Files.readAllLines(Paths.get("$dir${File.separator}MANIFEST.txt")).forEach {
                println(it)
            }
        }

        private inline fun cleanUp(dir: Path) = FileHandler.recursivelyDelete(dir)
    }

    fun jarIt(input: Array<String>, output: String, mainClass: String, version: String, useCompression: Boolean): Boolean {
        return if (isRunningAtomic.get()) {
            false
        } else {
            val jarData = JarData(input, output, mainClass, version, useCompression)
            jarDataAtomic.set(jarData)

            if (jarData.isOK) {
                jarIt()
                true
            } else {
                isOkAtomic.set(false)
                setNotRunning()
                false
            }
        }
    }

    private fun jarIt() {
        setRunning()

        ThreadUtils.run {
            val tempDir = Files.createTempDirectory("jari-temp")
            val tempFile = tempDir.toFile()
            val tempString = tempDir.toString()
            val jarData = jarDataAtomic.get()

            val inputArgs = mutableListOf<String>()

            val iter = jarData.input!!.iterator()

            for (inp in iter) {
                val inputPath = Paths.get(inp)
                inputPath.toFile().copyRecursively(File(tempString + File.separator + inputPath.name),
                    true) { _, exception -> throw exception }
                inputArgs.add(inputPath.name)
            }

            val mainClass = if (jarData.mainClasspath.isNullOrBlank()) null else jarData.mainClasspath

            if (mainClass != null) {
                val fullClasspath = tempString + File.separator + jarData.mainClasspath + ".class"
                println("107")

                if (!Files.exists(Paths.get(fullClasspath))) {
                    println("109")
                    jarData.errors.add(IOError("\"${jarData.mainClasspath}\"", JarData.ErrorState.NonExistent, "Main Class-Path"))
                    cleanUp(tempDir)
                    isOkAtomic.set(false)
                    setNotRunning()
                    return
                }
            }

            println("120")

            writeManifest(tempString, mainClass?.replace(File.separatorChar, '.'), jarData.version)
            val stopwatch = geo.timing.Stopwatch()
            stopwatch.tick()
            val process = invokeJar(tempFile, inputArgs.toTypedArray(), jarData.output!!, jarData.useCompression!!)
            process.waitFor()
            val time = stopwatch.tick()

            Fys.logger.debug("invokeJar time: ${time.asMillis().asRaw()}")

            cleanUp(tempDir)

            if (process.exitValue() != 0) {
                var error = ""

                for (str in process.errorStream.bufferedReader().lines()) {
                    error += str
                }

                jarData.errors.add(JarError(error))
                isOkAtomic.set(false)
            } else {
                isOkAtomic.set(true)
            }

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