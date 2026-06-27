package com.sentinela.camtv.capture

import android.net.Uri
import android.os.Build
import androidx.media3.ui.PlayerView
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.first

class CaptureRepository(
    private val settingsRepository: SettingsRepository,
    private val frameCaptureEngine: FrameCaptureEngine,
    private val captureStorage: CaptureStorage,
    private val fileNameFormatter: CaptureFileNameFormatter = CaptureFileNameFormatter(),
    private val customPhotoLocationEnabled: Boolean = true,
) {
    suspend fun takePhoto(
        request: CaptureRequest,
        playerView: PlayerView?,
    ): CaptureResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return CaptureResult.Failure(CaptureError.UnsupportedAndroid)
        }
        if (!request.renderedFirstFrame) {
            return CaptureResult.Failure(CaptureError.FirstFrameMissing)
        }
        val targetPlayerView = playerView ?: return CaptureResult.Failure(CaptureError.NoCaptureSource)
        val frame = frameCaptureEngine.capture(targetPlayerView)
        if (frame is FrameCaptureResult.Failure) {
            return CaptureResult.Failure(frame.error)
        }
        val bitmap = (frame as FrameCaptureResult.Success).bitmap
        val fileName = fileNameFormatter.photoFileName(request.cameraName)
        val customTreeUri = if (customPhotoLocationEnabled) {
            settingsRepository.observePreferences()
                .first()
                .photoCaptureTreeUri
                ?.let(Uri::parse)
        } else {
            null
        }
        val destination = PhotoCaptureDestination(customTreeUri)

        return try {
            when (val saved = captureStorage.savePhoto(bitmap, fileName, destination)) {
                is CaptureStorageResult.Success -> CaptureResult.Success(
                    fileName = saved.fileName,
                    locationLabel = saved.locationLabel,
                    uri = saved.uri,
                )
                is CaptureStorageResult.Failure -> CaptureResult.Failure(saved.error)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
