package com.sentinela.camtv.ui.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.SentinelaApplication
import com.sentinela.camtv.di.SentinelaViewModelFactory
import com.sentinela.camtv.ui.cameras.CameraManagerScreen
import com.sentinela.camtv.ui.cameras.CameraManagerViewModel
import com.sentinela.camtv.ui.capturegallery.CapturesScreen
import com.sentinela.camtv.ui.capturegallery.CapturesViewModel
import com.sentinela.camtv.ui.home.HomeScreen
import com.sentinela.camtv.ui.mosaic.MosaicScreen
import com.sentinela.camtv.ui.settings.SettingsScreen
import com.sentinela.camtv.ui.settings.SettingsViewModel
import com.sentinela.camtv.ui.theme.SentinelaBackground

@Composable
fun SentinelaAppScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as SentinelaApplication
    val viewModelFactory = remember(application.container) {
        SentinelaViewModelFactory(application.container)
    }
    val activity = remember(context) { context.findActivity() }
    val appViewModel: AppViewModel = viewModel(factory = viewModelFactory)
    val appState by appViewModel.state.collectAsState()

    when (appState.destination) {
        AppDestination.Loading -> LoadingScreen()
        AppDestination.Home -> {
            HomeScreen(
                onOpenMosaic = appViewModel::openMosaic,
                onOpenCameras = appViewModel::openCameras,
                onOpenCaptures = appViewModel::openCaptures,
                onOpenSettings = appViewModel::openSettings,
            )
        }
        AppDestination.Mosaic -> MosaicScreen(
            viewModelFactory = viewModelFactory,
            onOpenHome = appViewModel::openHome,
            onExitApp = {
                activity?.finishAndRemoveTask() ?: activity?.finish()
            },
            captureRepository = application.container.captureRepository,
            recordingProbeRepository = application.container.recordingProbeRepository,
        )
        AppDestination.Cameras -> {
            val cameraManagerViewModel: CameraManagerViewModel = viewModel(factory = viewModelFactory)
            val cameraManagerState by cameraManagerViewModel.state.collectAsState()
            CameraManagerScreen(
                state = cameraManagerState,
                onDiscoverOnvif = cameraManagerViewModel::discoverOnvifDevices,
                onSelectOnvifDevice = cameraManagerViewModel::selectDiscoveredDevice,
                onUsernameChanged = cameraManagerViewModel::updateUsername,
                onPasswordChanged = cameraManagerViewModel::updatePassword,
                onSaveSelectedOnvifCamera = cameraManagerViewModel::saveSelectedOnvifCamera,
                onRtspNameChanged = cameraManagerViewModel::updateRtspName,
                onRtspMainUrlChanged = cameraManagerViewModel::updateRtspMainUrl,
                onRtspSubUrlChanged = cameraManagerViewModel::updateRtspSubUrl,
                onRtspUsernameChanged = cameraManagerViewModel::updateRtspUsername,
                onRtspPasswordChanged = cameraManagerViewModel::updateRtspPassword,
                onCopyRtspMainUrlToSubUrl = cameraManagerViewModel::copyRtspMainUrlToSubUrl,
                onConnectManualRtspCamera = cameraManagerViewModel::connectManualRtspCamera,
                onDismissAuthDialog = cameraManagerViewModel::dismissAuthDialog,
                onOpenMosaic = appViewModel::openMosaic,
                onSelectActiveMosaic = cameraManagerViewModel::selectActiveMosaic,
                onPlaceCameraInMosaic = cameraManagerViewModel::placeCameraInMosaic,
                onRemoveCameraFromMosaic = cameraManagerViewModel::removeCameraFromMosaic,
                onBack = appViewModel::goBack,
            )
        }
        AppDestination.Captures -> {
            val capturesViewModel: CapturesViewModel = viewModel(factory = viewModelFactory)
            val capturesState by capturesViewModel.state.collectAsState()
            val treeLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri == null) {
                    capturesViewModel.showCustomPhotoLocationError()
                    return@rememberLauncherForActivityResult
                }
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }.onSuccess {
                    capturesViewModel.setCustomPhotoLocation(uri)
                }.onFailure {
                    capturesViewModel.showCustomPhotoLocationError()
                }
            }

            CapturesScreen(
                state = capturesState,
                onChoosePhotoLocation = {
                    if (context.canOpenDocumentTree()) {
                        treeLauncher.launch(null)
                    } else {
                        capturesViewModel.showPhotoLocationPickerUnavailable()
                    }
                },
                onUseDefaultPhotoLocation = capturesViewModel::useDefaultPhotoLocation,
                onMessageTimeout = capturesViewModel::clearMessage,
                onOpenHome = appViewModel::openHome,
                onBack = appViewModel::goBack,
                customPhotoLocationEnabled = BuildConfig.DEBUG,
            )
        }
        AppDestination.Settings -> {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val settingsState by settingsViewModel.state.collectAsState()
            SettingsScreen(
                state = settingsState,
                onExportSupportLogs = settingsViewModel::exportSupportLogs,
                onExportCrashReport = settingsViewModel::exportCrashReport,
                onCheckForUpdate = settingsViewModel::checkForUpdate,
                onDownloadUpdate = settingsViewModel::downloadUpdate,
                onInstallDownloadedUpdate = settingsViewModel::installDownloadedUpdate,
                onResumeAfterUpdatePermission = settingsViewModel::retryInstallerAfterPermissionResume,
                onDismissUpdateDialog = settingsViewModel::dismissUpdateDialog,
                onOpenHome = appViewModel::openHome,
                onBack = appViewModel::goBack,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.canOpenDocumentTree(): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    return intent.resolveActivity(packageManager) != null
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SentinelaBackground),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "Carregando...",
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
