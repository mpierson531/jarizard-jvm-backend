package com.jari

import geo.Fys
import geo.collections.FysList
import geo.files.FileHandler
import geo.threading.ThreadUtils
import geo.utils.Utils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.name

class Backend {
    enum class FileState {
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

    private var isDoneAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var isOkAtomic: AtomicReference<Boolean> = AtomicReference(false)
    private var errorsAtomic: AtomicReference<FysList<IOError>>? = null
    private var isRunningAtomic: AtomicReference<Boolean> = AtomicReference(false)

    val isDone: Boolean get() = isDoneAtomic.get()
    val isOk: Boolean get() = isOkAtomic.get()
    val errors: FysList<IOError> get() {
        val errs = errorsAtomic!!.get()
        errorsAtomic = null
        return errs
    }

    private var input: Array<String>? = null
    private var output: String? = null

    private val queue: AtomicReference<FysList<Pair<Array<String>, String>>> = AtomicReference(FysList())

    companion object {
        private inline fun invokeJar(directory: File, output: String, input: Array<String>): Process {
            // TODO: INTRODUCE MORE ERROR HANDLING HERE

            val jar = "${System.getenv("JAVA_HOME")}${File.separator}bin${File.separator}jar.exe"

            val command = ArrayList<String>(input.size + 3)
            command.add(jar)
            command.add("cf")
            command.add("\"$output\"")
            command.addAll(Array(input.size) { "\"${input[it]}\"" })

            return ProcessBuilder(arrayListOf(jar, "cf", "\"$output\"", *Array(input.size) { "\"${input[it]}\"" }))
                .directory(directory)
                .start()
        }

        private inline fun cleanUp(dir: Path) = FileHandler.recursivelyDelete(dir)

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

        private inline fun validate(dir: String): FileState {
            return if (dir.length == 1) {
                FileState.SingleChar
            } else if (dir.isEmpty() || dir == "") {
                FileState.Empty
            } else if (!dir.contains(File.separator, true)) {
                FileState.SeparatorNotPresent
            } else if (!Files.exists(Paths.get(dir).toAbsolutePath())) {
                FileState.NonExistent
            } else {
                FileState.Ok
            }
        }
    }

    private inline fun addError(dir: String, state: FileState, isInput: Boolean) {
        if (errorsAtomic == null)
            errorsAtomic = AtomicReference(FysList())

        errorsAtomic!!.get().add(IOError(dir, state, isInput))
    }

    private fun validateAndSanitize(): Boolean {
        output = sanitize(output!!)

        val outputState = validate(output!!)
        var isOk = true

        if (outputState != FileState.NonExistent && outputState != FileState.Ok) {
            isOk = false
            addError(output!!, outputState, false)
        }

        val input = this.input!!

        if (input.size == 1) {
            input[0] = sanitize(input[0])
            val validation = validate(input[0])

            if (validation != FileState.Ok) {
                isOk = false
                addError(input[0], validation, true)
            }
        } else {
            for (i in 0..input.size - 1) {
                input[i] = sanitize(input[i])
                val validation = validate(input[i])

                if (validation != FileState.Ok) {
                    isOk = false
                    addError(input[i], validation, true)
                }
            }
        }

        return isOk
    }

    private tailrec fun run() {
        if (queue.get().isEmpty) {
            input = null
            output = null
            return
        }

        val next = queue.get().remove(0)
        input = next.first
        output = next.second

        if (validateAndSanitize()) {
            val tempDir = Files.createTempDirectory("jari-temp")
            val tempFile = tempDir.toFile()
            val tempString = tempDir.toString()

            val inputArgs = mutableListOf<String>()

            val iter = input!!.iterator()

            for (inp in iter) {
                val inputPath = Paths.get(inp)
                inputPath.toFile()
                    .copyRecursively(File(tempString + File.separator + inputPath.name), true) { _, exception -> throw exception }
                inputArgs.add(inputPath.name)
            }

            val stopwatch = geo.timing.Stopwatch()
            stopwatch.tick()
            invokeJar(tempFile, output!!, inputArgs.toTypedArray()).waitFor()
            val time = stopwatch.tick()
            Fys.logger.debug("invokeJar time: ${time.asMillis().asRaw()}")

            cleanUp(tempDir)

            isOkAtomic.set(true)
        } else {
            isOkAtomic.set(false)
        }

        isDoneAtomic.set(true)
        isRunningAtomic.set(false)
        run()
    }

    fun queue(nextInput: Array<String>, nextOutput: String) {
        if (!isRunningAtomic.get()) {
            queue.get().add(Pair(nextInput, nextOutput))
            isRunningAtomic.set(true)
            ThreadUtils.run { run() }
        } else {
            synchronized(queue) {
                queue.get().add(Pair(nextInput, nextOutput))
            }
        }
    }
}