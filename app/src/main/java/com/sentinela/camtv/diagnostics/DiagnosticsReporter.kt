package com.sentinela.camtv.diagnostics

interface DiagnosticsReporter {
    fun setEnabled(enabled: Boolean)
    fun setKey(key: String, value: String)
    fun log(message: String)
    fun recordNonFatal(throwable: Throwable, message: String? = null)
}
