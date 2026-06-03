package com.sentinela.camtv.diagnostics

class NoOpDiagnosticsReporter : DiagnosticsReporter {
    override fun setEnabled(enabled: Boolean) = Unit
    override fun setKey(key: String, value: String) = Unit
    override fun log(message: String) = Unit
    override fun recordNonFatal(throwable: Throwable, message: String?) = Unit
}
