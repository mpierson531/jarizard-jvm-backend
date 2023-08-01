package com.jari.backend.errors

class NaNError(value: String, isVersion: Boolean) : DataError {
    override val value: String = if (isVersion) {
        "Compression: $value, not a number"
    } else {
        "Version: $value, not a number"
    }

    override inline fun toString(): String = value
}