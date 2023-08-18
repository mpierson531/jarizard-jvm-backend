package com.jari.backend.errors

import com.jari.backend.Backend

class IOError(dir: String?, error: Backend.ErrorState, prefix: String) : DataError() {
    companion object {
        private inline fun format(value: String?, error: Backend.ErrorState): String {
            return if (error == Backend.ErrorState.Empty) {
                "*empty*, $error"
            } else {
                "\"$value\", $error"
            }
        }
    }

    override val value: String = "$prefix: ${format(dir, error)}"
}

class NaNError(value: String, isVersion: Boolean) : DataError() {
    override val value: String = if (isVersion) {
        "Compression: \"$value\", not a number"
    } else {
        "Version: \"$value\", not a number"
    }
}