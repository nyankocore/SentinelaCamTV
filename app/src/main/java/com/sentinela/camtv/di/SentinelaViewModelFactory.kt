package com.sentinela.camtv.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.ui.app.AppViewModel
import com.sentinela.camtv.ui.capturegallery.CapturesViewModel
import com.sentinela.camtv.ui.cameras.CameraManagerViewModel
import com.sentinela.camtv.ui.home.HomeViewModel
import com.sentinela.camtv.ui.mosaic.MosaicViewModel
import com.sentinela.camtv.ui.player.FullscreenPlayerViewModel
import com.sentinela.camtv.ui.settings.SettingsViewModel

class SentinelaViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AppViewModel::class.java -> AppViewModel(container.cameraRepository)
        HomeViewModel::class.java -> HomeViewModel(container.cameraRepository)
        MosaicViewModel::class.java -> MosaicViewModel(
            cameraRepository = container.cameraRepository,
            mosaicLayoutRepository = container.mosaicLayoutRepository,
            settingsRepository = container.settingsRepository,
        )
        FullscreenPlayerViewModel::class.java -> FullscreenPlayerViewModel(
            settingsRepository = container.settingsRepository,
        )
        CapturesViewModel::class.java -> CapturesViewModel(
            settingsRepository = container.settingsRepository,
            customPhotoLocationEnabled = BuildConfig.DEBUG,
        )
        CameraManagerViewModel::class.java -> CameraManagerViewModel(
            cameraRepository = container.cameraRepository,
            mosaicLayoutRepository = container.mosaicLayoutRepository,
            settingsRepository = container.settingsRepository,
            onvifRepository = container.onvifRepository,
            rtspConnectionTester = container.rtspConnectionTester,
            rtspCameraDraftRepository = container.rtspCameraDraftRepository,
        )
        SettingsViewModel::class.java -> SettingsViewModel(
            logRepository = container.logRepository,
            updateRepository = container.updateRepository,
            appUpdateInstaller = container.appUpdateInstaller,
        )
        else -> error("ViewModel sem factory manual: ${modelClass.name}")
    } as T
}
