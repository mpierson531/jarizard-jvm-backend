package com.jari

import geo.Fys
import geo.collections.FysList
import geo.files.FileHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

class Backend(@JvmField var input: FysList<String>, output: String?) {
    enum class State {
        Empty,
        NonExistent,
        Ok
    }

    @JvmField var output: String? = when {
        output.isNullOrEmpty() -> null
        !output.contains(".") -> "$output.jar"
        else -> output
    }

    constructor() : this(FysList(), "")

    companion object {
        private fun invokeJar(directory: String,
                              output: String,
                              vararg input: String): Process { // TODO: INTRODUCE MORE EXCEPTION HANDLING

            val javaHome = System.getenv("JAVA_HOME")

            val jar = "$javaHome${File.separator}bin${File.separator}jar.exe"

            val command = ArrayList<String>(input.size + 3)
            command.add(jar)
            command.add("cf")
            command.add("\"$output\"")
            command.addAll(input.map { "\"$it\"" })

            val builder = ProcessBuilder(command)

            builder.inheritIO().directory(Paths.get(directory).toFile())

            return builder.start()
        }

        private inline fun isolateDirectory(dir: String): String = Paths.get(dir).name

        private fun cleanUp(dir: String) {
            recursivelyDelete(Paths.get(dir))
        }

        private fun recursivelyDelete(dir: Path) {
            if (Fys.isDebug)
                Fys.logger.debug(dir)

            if (Files.isDirectory(dir)) {
                for (fsObj in Files.newDirectoryStream(dir)) {
                    recursivelyDelete(fsObj)
                }
            }

            Files.delete(dir)
        }

        private fun sanitize(dir: String): String {
            val dir = dir.trim().split(File.separator).toMutableList()

            var string = ""

            if (dir[dir.size - 1] == File.separator) {
                while (dir[dir.size - 1] == File.separator) {
                    dir.removeAt(dir.size - 1)
                }
            }

            for (it in dir) {
                string += it + File.separator
            }

            return string
        }
    }

    private fun validate(dir: String): State {
        return if (dir.isEmpty()) {
            State.Empty
        } else if (!Files.exists(Paths.get(dir).toAbsolutePath())) {
            State.NonExistent
        } else {
            State.Ok
        }
    }

    private fun validateAndSanitize(): State {
        input.filterNulls()
        output = output?.trim()

        if (input.size == 1) {
            input[0] = sanitize(input[0])
            return validate(input[0])
        } else {
            for (i in 0..input.size - 1) {
                input[i] = sanitize(input[i])

                return when (validate(input[i])) {
                    State.Empty -> State.Empty
                    State.NonExistent -> State.NonExistent
                    State.Ok -> continue
                }
            }
        }

        return State.Ok
    }

    private fun copyInputDirs(): Path {
        val tempDir = Files.createTempDirectory("jari-temp")
        val tempFile = tempDir.toFile()

        for (inputDir in input) {
            val file = Paths.get(inputDir).parent.toAbsolutePath().toFile()
            file.copyRecursively(tempFile, true) { _, exception -> throw exception }
        }

        return tempDir
    }

    fun jarizard(): Backend.State {
        return when (validateAndSanitize()) {
            State.Empty -> State.Empty
            State.NonExistent -> State.NonExistent
            State.Ok -> {
                val tempDir = copyInputDirs()

                val inputArgs = mutableListOf<String>()

                for (inp in input) {
                    inputArgs.add(isolateDirectory(inp))
                }

                invokeJar(tempDir.toAbsolutePath().toString(), output!!.trim(), *inputArgs.toTypedArray()).waitFor()

                cleanUp(tempDir.toAbsolutePath().toString())

                State.Ok
            }
        }
    }
}