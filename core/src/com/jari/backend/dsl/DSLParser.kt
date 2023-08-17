package com.jari.backend.dsl

import com.jari.backend.JResult
import com.jari.backend.JarData
import com.jari.backend.dsl.DSLParser.DSLType.*
import com.jari.backend.dsl.Token.Type
import com.jari.backend.errors.*
import java.io.File

internal class DSLParser(private val tokens: MutableList<Token>) {
    private var isOk: Boolean = false
    private var lineNumber: Int = tokens[0].lineNumber

    private enum class DSLType {
        Input,
        Output,
        MainClass,
        Version,
        Dependency,
        UseCompression,
        Unknown;

        override fun toString(): String = this.name
    }

    companion object {
        private val keywords: Array<String> = arrayOf(
                "in", "input",
                "out", "output",
                "main", "main-class", "mainclass",
                "vers", "version",
                "dep", "dependency", "deps", "dependencies",
                "comp", "compress", "compression", "useCompression", "usecompression"
            )

        private val inputKeys = arrayOf("in", "input")
        private val outputKeys = arrayOf("out", "output")
        private val mainClassKeys = arrayOf("main", "main-class", "mainclass")
        private val versionKeys = arrayOf("vers", "version")
        private val dependencyKeys = arrayOf("dep", "dependency", "deps", "dependencies")
        private val compressionKeys = arrayOf("comp", "compress", "compression", "useCompression", "usecompression")

        private fun isInput(token: Token): Boolean = inputKeys.contains(token.value)
        private fun isOutput(token: Token): Boolean = outputKeys.contains(token.value)
        private fun isMainClass(token: Token): Boolean = mainClassKeys.contains(token.value)
        private fun isVersion(token: Token): Boolean = versionKeys.contains(token.value)
        private fun isDependency(token: Token): Boolean = dependencyKeys.contains(token.value)
        private fun isCompression(token: Token): Boolean = compressionKeys.contains(token.value)

        fun parse(file: File): JResult<JarData, Array<DataError>> = parse(file.readText())
        fun parse(text: String): JResult<JarData, Array<DataError>> = DSLParser(Lexer.lex(text)).parse()

        private fun getDSLType(value: String): DSLType = when (value) {
            "input" -> Input
            "output" -> Output
            "mainclass", "main-class", "main" -> MainClass
            "version", "vers" -> Version
            "dependency", "dependencies" -> Dependency
            "compress", "compression", "use-compression", "useCompression" -> UseCompression
            else -> Unknown
        }
    }

    private fun parse(): JResult<JarData, Array<DataError>> {
        isOk = true

        val input: MutableList<String> = ArrayList(16)
        var output = ""
        val dependencies = ArrayList<Pair<String, String>>()
        var mainClass = ""
        var version = ""
        var useCompression = true
        val errors: MutableList<DataError> = ArrayList(4)

        val parseWhileCondition: (Token) -> Boolean = { it.type == Type.Value }

        val dependencyParse: (MutableList<Char>) -> String = { chars ->
            var inQuotes = false
            var string = ""

            while (chars.isNotEmpty()) {
                var char = chars.removeAt(0)

                if (char == '\n') {
                    lineNumber++
                }

                if (char == '"') {
                    inQuotes = !inQuotes
                    char = chars.removeAt(0)
                }

                if (char == '\n') {
                    lineNumber++
                }

                string += if (char.isWhitespace()) {
                    if (inQuotes) char
                    else break
                } else {
                    char
                }
            }

            string
        }

        val dependencyFunction: () -> Unit = {
            val leftBrace = eatIf { it.type == Type.LeftBrace }

            val rawDependencies = parseWhile(Dependency, parseWhileCondition).toMutableList()

            if (leftBrace.first) {
                eatExpectantly(errors, Type.RightBrace)
            }

            while (rawDependencies.isNotEmpty()) {
                val rawDependency = rawDependencies.removeAt(0).toCharArray().toMutableList()

                val depPath = dependencyParse(rawDependency)
                val depVersion = dependencyParse(rawDependency)

                dependencies.add(Pair(depPath, depVersion))
            }
        }

        while (tokens.isNotEmpty()) {
            val token = eat()

            when (token.type) {
                Type.Value -> {
                    if (isInput(token)) {
                        val leftBrace = eatIf { it.type == Type.LeftBrace }

                        input.addAll(parseWhile(Input, parseWhileCondition))

                        if (leftBrace.first) {
                            eatExpectantly(errors, Type.RightBrace)
                        }
                    } else if (isOutput(token)) {
                        output = parseWhile(Output, parseWhileCondition)[0]
                    } else if (isMainClass(token)) {
                        mainClass = parseWhile(MainClass, parseWhileCondition)[0]
                    } else if (isCompression(token)) {
                        val next = eat()

                        if (next.value == "false" || next.value == "0") {
                            useCompression = false
                        }
                    } else if (isDependency(token)) {
                        dependencyFunction.invoke()
                    } else if (isVersion(token)) {
                        version = parseWhile(Version, parseWhileCondition)[0]
                    } else {
                        if (isOk) {
                            isOk = false
                        }

                        errors.add(UnexpectedValueErr(token.value, token.lineNumber))
                    }
                }
                else -> {
                    if (isOk) {
                        isOk = false
                    }

                    errors.add(UnexpectedValueErr(token.value, token.lineNumber))
                }
            }
        }

        println("useCompression = $useCompression")

        if (!isOk) {
            return JResult.err(errors.toTypedArray())
        }

        return JResult.ok(JarData(input.toTypedArray(), output, dependencies.toTypedArray(), mainClass, version, useCompression))
    }

    private inline fun eatIf(condition: (Token) -> Boolean): Pair<Boolean, Token?> {
        return if (condition.invoke(peek())) {
            true to eat()
        } else {
            false to null
        }
    }

    private fun eatExpectantly(errors: MutableList<DataError>, expectedType: Type): Boolean {
        return if (isEmpty()) {
            isOk = false
            lineNumber++

            val error = if (expectedType.isRaw()) {
                ExpectedRawErr("'EOF'", expectedType, lineNumber)
            } else {
                ExpectedValueErr("'EOF'", expectedType.toRawValue(), expectedType.toString(), lineNumber)
            }

            errors.add(error)
        } else {
            val next = eat()

            if (next.type != expectedType) {
                isOk = false
                errors.add(ExpectedValueErr(next.value, expectedType.toString(), expectedType.toString(), next.lineNumber))
                false
            } else {
                true
            }
        }
    }

    private inline fun parseWhile(currentDSLType: DSLType, condition: (Token) -> Boolean): List<String> {
        if (isEmpty()) return ArrayList(0)

        var token = peek()
        val list = ArrayList<String>(24)

        while (condition.invoke(token)) {
            if (keywords.contains(token.value)) {
                if (getDSLType(token.value) != currentDSLType) {
                    break
                }

                eat()
                token = peek()
            } else {
                eat()
            }

            if (token.type == Type.LeftBrace) {
                token = peek()
                continue
            }

            list.add(token.value)

            if (isEmpty()) {
                break
            }

            token = peek()
        }

        return list
    }

    private inline fun eat(): Token {
        val next = tokens.removeAt(0)
        lineNumber = next.lineNumber
        return next
    }

    private inline fun peek(): Token = tokens[0]

    private inline fun isEmpty(): Boolean = tokens.isEmpty()
    private inline fun isNotEmpty(): Boolean = tokens.isNotEmpty()
}