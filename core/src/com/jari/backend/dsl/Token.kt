package com.jari.backend.dsl

internal data class Token(val type: Type, val value: String, val lineNumber: Int) {
    enum class Type {
        Equals,
        LeftBrace,
        RightBrace,
        Quote,
        Break,
        Value;

        override fun toString(): String = when (this) {
            Equals -> "\"=\""
            LeftBrace -> "\"{\""
            RightBrace -> "\"}\""
            Quote -> "'\"'"
            Break -> "'*break*' (whitespace)"
            Value -> "'*value*'"
        }
    }

    override fun toString(): String = when (this.type) {
        Type.Value -> this.value
        else -> "Token={type=${this.type}, value=${this.value}}"
    }
}