package com.sentinela.camtv

import android.app.Application
import com.sentinela.camtv.config.defaultMosaicCameras
import com.sentinela.camtv.di.AppContainer
import com.sentinela.camtv.diagnostics.DiagnosticsTimberTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SentinelaApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Timber.plant(Timber.DebugTree())
        Timber.plant(container.fileTimberTree)
        Timber.plant(DiagnosticsTimberTree(container.diagnosticsReporter))
        container.crashReporter.install()
        container.entitlementRepository.start()
        appScope.launch {
            container.settingsRepository.observePreferences()
                .map { preferences -> preferences.diagnosticsEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    container.diagnosticsReporter.setEnabled(enabled)
                }
        }

        if (BuildConfig.SEED_DEBUG_CAMERAS) {
            runBlocking(Dispatchers.IO) {
                container.cameraRepository.seedDebugCamerasIfEmpty(defaultMosaicCameras())
            }
        }
    }
}
