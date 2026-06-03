package com.sentinela.camtv.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseDiagnosticsReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : DiagnosticsReporter {
    override fun setEnabled(enabled: Boolean) {
        runCatching {
            crashlytics.setCrashlyticsCollectionEnabled(enabled)
        }
    }

    override fun setKey(key: String, value: String) {
        runCatching {
            crashlytics.setCustomKey(key, DiagnosticsSanitizer.sanitize(value))
        }
    }

    override fun log(message: String) {
        runCatching {
            crashlytics.log(DiagnosticsSanitizer.sanitize(message))
        }
    }

    override fun recordNonFatal(throwable: Throwable, message: String?) {
        runCatching {
            message?.let { crashlytics.log(DiagnosticsSanitizer.sanitize(it)) }
            crashlytics.recordException(DiagnosticsSanitizer.sanitizeThrowable(throwable))
        }
    }
}
