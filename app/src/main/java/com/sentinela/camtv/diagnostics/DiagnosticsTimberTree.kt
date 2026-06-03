package com.sentinela.camtv.diagnostics

import android.util.Log
import timber.log.Timber

class DiagnosticsTimberTree(
    private val reporter: DiagnosticsReporter,
) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN && t == null) return

        val safeMessage = buildString {
            if (!tag.isNullOrBlank()) {
                append(tag)
                append(": ")
            }
            append(message)
        }
        reporter.log(safeMessage)
        if (t != null || priority >= Log.ERROR) {
            reporter.recordNonFatal(t ?: RuntimeException(safeMessage))
        }
    }
}
