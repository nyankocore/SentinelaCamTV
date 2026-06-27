package com.sentinela.camtv.capture

import android.net.Uri

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

fun CaptureResult.userMessage(): String = when (this) {
    is CaptureResult.Success -> "Foto salva."
    is CaptureResult.Failure -> error.userMessage()
}

fun CaptureError.userMessage(): String = when (this) {
    CaptureError.UnsupportedAndroid -> "Captura de foto indisponível neste Android."
    CaptureError.FirstFrameMissing -> "Aguarde a imagem da câmera aparecer para tirar foto."
    CaptureError.NoCaptureSource,
    CaptureError.CaptureFailed,
    -> "Não foi possível capturar a imagem da câmera agora."
    CaptureError.SourceNoData -> "Não foi possível capturar a imagem da câmera agora."
    CaptureError.CustomLocationUnavailable ->
        "Não foi possível salvar nesse local. Escolha outra pasta ou use o local padrão."
    CaptureError.WriteFailed -> "Não foi possível salvar a foto."
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
