package com.sentinela.camtv.di

import android.content.Context
import androidx.room.Room
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.billing.DebugBillingRepository
import com.sentinela.camtv.billing.PlayBillingRepository
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.data.camera.RoomCameraRepository
import com.sentinela.camtv.data.db.SentinelaDatabase
import com.sentinela.camtv.data.onvif.AndroidWsDiscoveryClient
import com.sentinela.camtv.data.onvif.DefaultOnvifRepository
import com.sentinela.camtv.data.onvif.OnvifRepository
import com.sentinela.camtv.data.security.CredentialCipher
import com.sentinela.camtv.diagnostics.DiagnosticsReporter
import com.sentinela.camtv.diagnostics.FirebaseDiagnosticsReporter
import com.sentinela.camtv.diagnostics.NoOpDiagnosticsReporter
import com.sentinela.camtv.entitlement.CommercialEntitlementRepository
import com.sentinela.camtv.entitlement.EntitlementRepository
import com.sentinela.camtv.logging.CrashReporter
import com.sentinela.camtv.logging.FileTimberTree
import com.sentinela.camtv.logging.LogRepository
import com.sentinela.camtv.logging.LocalLogRepository
import com.sentinela.camtv.player.Media3RtspConnectionTester
import com.sentinela.camtv.player.RtspConnectionTester
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.camtv.preferences.playerPreferencesRepository
import com.sentinela.camtv.ui.cameras.RtspCameraDraftRepository
import com.sentinela.camtv.ui.cameras.rtspCameraDraftRepository
import com.sentinela.onvif.OnvifSoapClient

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val database: SentinelaDatabase = Room.databaseBuilder(
        appContext,
        SentinelaDatabase::class.java,
        "sentinela.db",
    ).build()

    val fileTimberTree: FileTimberTree = FileTimberTree(appContext)
    val crashReporter: CrashReporter = CrashReporter(appContext)

    val logRepository: LogRepository = LocalLogRepository(
        context = appContext,
        fileTimberTree = fileTimberTree,
        crashReporter = crashReporter,
    )

    private val credentialCipher = CredentialCipher()

    val cameraRepository: CameraRepository = RoomCameraRepository(
        cameraDao = database.cameraDao(),
        credentialCipher = credentialCipher,
    )

    val settingsRepository: SettingsRepository = playerPreferencesRepository(appContext)

    private val billingRepository = if (BuildConfig.DEBUG) {
        DebugBillingRepository(BuildConfig.DEBUG_ENTITLEMENT)
    } else {
        PlayBillingRepository(appContext)
    }

    val entitlementRepository: EntitlementRepository = CommercialEntitlementRepository(
        billingRepository = billingRepository,
        settingsRepository = settingsRepository,
    )

    val diagnosticsReporter: DiagnosticsReporter = if (BuildConfig.CRASHLYTICS_CONFIGURED) {
        runCatching { FirebaseDiagnosticsReporter() }.getOrElse { NoOpDiagnosticsReporter() }
    } else {
        NoOpDiagnosticsReporter()
    }

    val rtspCameraDraftRepository: RtspCameraDraftRepository =
        rtspCameraDraftRepository(appContext)

    val rtspConnectionTester: RtspConnectionTester = Media3RtspConnectionTester(appContext)

    val onvifRepository: OnvifRepository = DefaultOnvifRepository(
        wsDiscoveryClient = AndroidWsDiscoveryClient(appContext),
        soapClient = OnvifSoapClient(),
    )
}
