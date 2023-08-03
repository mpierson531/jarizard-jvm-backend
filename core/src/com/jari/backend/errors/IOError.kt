package com.jari.backend.errors

import com.jari.backend.Backend.ErrorState

class IOError(dir: String?, error: ErrorState, prefix: String) : DataError {
    companion object {
        private inline fun format(value: String?, error: ErrorState): String {
            return if (error == ErrorState.Empty) {
                "*empty*, $error"
            } else {
                "\"$value\", $error"
            }
        }
    }

    override val value: String = "$prefix: ${format(dir, error)}"
    override fun toString(): String = value
}