package com.jari.backend.dsl

import com.jari.backend.dsl.Token.Type.*

internal class Lexer {
    private var lineNumber: Int = 1
    private val chars: MutableList<Char> = ArrayList(5120)

    companion object {
        fun lex(string: String): MutableList<Token> = Lexer().tokenize(string)

        private inline fun getTokenType(char: String): Token.Type {
            if (char.isBlank()) {
                return Token.Type.Break
            }

            return when (char) {
                "{" -> LeftBrace
                "}" -> RightBrace
                "=" -> Equals
                "\"" -> Quote
                else -> Value
            }
        }
    }

    private fun tokenize(string: String): MutableList<Token> {
        this.chars.addAll(string.toCharArray().toMutableList())

        val tokens = ArrayList<Token>(chars.size / 2)

        while (chars.isNotEmpty()) {
            val char = eat()
            val charString = char.toString()

            when (getTokenType(charString)) {
                Equals, Break -> continue
                LeftBrace -> tokens.add(Token(LeftBrace, "{", lineNumber))
                RightBrace -> tokens.add(Token(RightBrace, "}", lineNumber))
                Quote -> {
                    tokens.add(Token(Value, eatWhile { getTokenType(it.toString()) != Quote }, lineNumber))
                }
                Value -> {
                    val string = charString + eatWhile {
                        val type = getTokenType(it.toString())
                        type != Break && type != Equals && type != LeftBrace && type != RightBrace
                    }

                    println("String: $string")

                    tokens.add(Token(Value, string, lineNumber))
                }
            }
        }

        return tokens
    }

    private fun eatWhile(condition: (Char) -> Boolean): String {
        var string = ""
        var char = eat()

        while (chars.isNotEmpty() && condition.invoke(char)) {
            string += char
            char = eat()
        }

        return string
    }

    private inline fun eat(): Char {
        val next = chars.removeAt(0)

        if (next == '\n') {
            lineNumber++
        }

        return next
    }
}