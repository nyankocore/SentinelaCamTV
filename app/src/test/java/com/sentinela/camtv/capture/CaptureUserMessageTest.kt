package com.sentinela.camtv.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureUserMessageTest {
    @Test
    fun unsupportedAndroidUsesClearMessage() {
        assertEquals(
            "Captura de foto indisponível neste Android.",
            CaptureError.UnsupportedAndroid.userMessage(),
        )
    }

    @Test
    fun firstFrameMissingAsksUserToWaitForImage() {
        assertEquals(
            "Aguarde a imagem da câmera aparecer para tirar foto.",
            CaptureError.FirstFrameMissing.userMessage(),
        )
    }

    @Test
    fun sourceNoDataUsesFriendlyFailure() {
        assertEquals(
            "Não foi possível capturar a imagem da câmera agora.",
            CaptureError.SourceNoData.userMessage(),
        )
    }

    @Test
    fun successMessageIsShort() {
        assertEquals(
            "Foto salva.",
            CaptureResult.Success(
                fileName = "foto.jpg",
                locationLabel = CaptureLocationLabels.STANDARD_PHOTOS,
                uri = null,
            ).userMessage(),
        )
    }
}
