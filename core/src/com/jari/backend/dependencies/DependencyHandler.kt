package com.jari.backend.dependencies

import com.jari.backend.Backend
import com.jari.backend.FsObj
import com.jari.backend.errors.DataError
import com.jari.backend.errors.IOError
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class DependencyHandler {
    private val errorsAtomic: AtomicReference<MutableList<DataError>> = AtomicReference(ArrayList())
    val errors: MutableList<DataError> get() = errorsAtomic.get()

    private val isOkAtomic: AtomicBoolean = AtomicBoolean(false)
    val isOk: Boolean get() = isOkAtomic.get()

    inline fun download(directory: String, dependencies: MutableList<Dependency>, forEach: (Dependency) -> Unit) {
        var isOk = true

        for (dependency in dependencies.toTypedArray()) {
            try {
                val result = dependency.getBufferedStream()

                if (result.isOk) {
                    val fileObj = FsObj("$directory${dependency.name}-${dependency.version}.jar")

                    result.unwrap().use { input ->
                        BufferedOutputStream(FileOutputStream(fileObj.file)).use { output ->
                            var next = input.read()

                            while (next != -1) {
                                output.write(next)
                                next = input.read()
                            }
                        }
                    }

                    forEach.invoke(dependency)
                } else {
                    isOk = false
                    errorsAtomic.get().add(IOError(result.unwrapErr().toString(), Backend.ErrorState.Exception, "Error"))
                }
            } catch (e: java.lang.Exception) {
                isOk = false
                errorsAtomic.get().add(IOError(e.message, Backend.ErrorState.Exception, "Error"))
            }
        }

        isOkAtomic.set(isOk)
    }
}