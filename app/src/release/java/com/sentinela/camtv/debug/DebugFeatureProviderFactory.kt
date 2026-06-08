package com.sentinela.camtv.debug

import android.content.Context
import com.sentinela.camtv.billing.BillingRepository
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.diagnostics.DiagnosticsReporter
import com.sentinela.camtv.entitlement.EntitlementRepository
import com.sentinela.camtv.logging.CrashReporter
import com.sentinela.camtv.logging.FileTimberTree
import com.sentinela.camtv.logging.LogRepository
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun createDebugFeatureProvider(
    context: Context,
    billingRepository: BillingRepository,
    entitlementRepository: EntitlementRepository,
    settingsRepository: SettingsRepository,
    cameraRepository: CameraRepository,
    logRepository: LogRepository,
    fileTimberTree: FileTimberTree,
    crashReporter: CrashReporter,
    diagnosticsReporter: DiagnosticsReporter,
): DebugFeatureProvider = NoOpDebugFeatureProvider

private object NoOpDebugFeatureProvider : DebugFeatureProvider {
    override val state: StateFlow<DebugFeatureState> = MutableStateFlow(DebugFeatureState())
    override fun openPanel() = Unit

    @androidx.compose.runtime.Composable
    override fun Render() = Unit
}
