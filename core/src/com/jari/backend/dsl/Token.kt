package com.jari.backend.dsl

data class Token(val type: Token.Type, val value: String, val lineNumber: Int) {
    enum class Type {
        Equals,
        LeftBrace,
        RightBrace,
        Quote,
        Break,
        Value;

        fun toRawValue(): String = when (this) {
            Equals -> "'='"
            LeftBrace -> "'{'"
            RightBrace -> "'}'"
            Quote -> "'\"'"
            else -> this.toString()
        }

        fun isRaw(): Boolean = when (this) {
            Equals, LeftBrace, RightBrace, Quote -> true
            else -> false
        }

        override fun toString(): String = when (this) {
            Equals -> "equals"
            LeftBrace -> "left-brace"
            RightBrace -> "right-brace"
            Quote -> "quotes"
            Break -> "break"
            Value -> "value"
        }
    }

    override fun toString(): String = when (this.type) {
        Type.Value -> this.value
        else -> "Token={type=${this.type}, value=${this.value}}"
    }
}