package com.jari.backend.errors

import com.jari.backend.JarData

class IOError(dir: String?, error: JarData.ErrorState, prefix: String) : DataError {
    companion object {
        private inline fun format(value: String?, error: JarData.ErrorState): String {
            return if (error == JarData.ErrorState.Empty) {
                "*empty*, $error"
            } else {
                "$value, $error"
            }
        }
    }

    override val value: String = "$prefix: ${format(dir, error)}"
    override inline fun toString(): String = value
}