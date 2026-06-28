package com.sentinela.camtv.capture

import android.net.Uri
import com.sentinela.camtv.R
import com.sentinela.camtv.ui.text.UiText

data class CaptureRequest(
    val cameraName: String,
    val renderedFirstFrame: Boolean,
)

sealed interface CaptureResult {
    data class Success(
        val fileName: String,
        val locationLabel: String,
        val uri: Uri?,
    ) : CaptureResult

    data class Failure(
        val error: CaptureError,
    ) : CaptureResult
}

enum class CaptureError {
    UnsupportedAndroid,
    FirstFrameMissing,
    NoCaptureSource,
    SourceNoData,
    CaptureFailed,
    CustomLocationUnavailable,
    WriteFailed,
}

fun CaptureResult.userMessage(): UiText = when (this) {
    is CaptureResult.Success -> UiText.Resource(R.string.capture_photo_saved)
    is CaptureResult.Failure -> error.userMessage()
}

fun CaptureError.userMessage(): UiText = when (this) {
    CaptureError.UnsupportedAndroid -> UiText.Resource(R.string.capture_unsupported_android)
    CaptureError.FirstFrameMissing -> UiText.Resource(R.string.capture_first_frame_missing)
    CaptureError.NoCaptureSource,
    CaptureError.CaptureFailed,
    -> UiText.Resource(R.string.capture_failed_now)
    CaptureError.SourceNoData -> UiText.Resource(R.string.capture_failed_now)
    CaptureError.CustomLocationUnavailable ->
        UiText.Resource(R.string.capture_custom_location_unavailable)
    CaptureError.WriteFailed -> UiText.Resource(R.string.capture_write_failed)
}

data class PhotoCaptureDestination(
    val customTreeUri: Uri?,
) {
    val usesCustomLocation: Boolean
        get() = customTreeUri != null
}

object CaptureLocationLabels {
    const val STANDARD_PHOTOS = "Imagens/Sentinela Cam TV"
    const val APP_EXTERNAL_PHOTOS = "Imagens do app/Sentinela Cam TV"
    const val CUSTOM_PHOTOS = "Pasta personalizada"

    fun defaultPhotoLocationLabel(sdkInt: Int): String =
        if (sdkInt >= 29) STANDARD_PHOTOS else APP_EXTERNAL_PHOTOS
}
