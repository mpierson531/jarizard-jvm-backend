package com.jari

data class IOError(val dir: String, val state: Backend.FileState, val isInput: Boolean) {
    override fun toString(): String = if (isInput) {
        "Input Directory: ${formatDir(dir)}, $state"
    } else {
        "Output Directory: ${formatDir(dir)}, $state"
    }

    private inline fun formatDir(dir: String): String {
       return if (dir.isBlank()) "*empty*" else "\"$dir\""
    }
}