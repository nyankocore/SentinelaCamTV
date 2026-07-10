package com.sentinela.camtv.ui.capturegallery

import com.sentinela.camtv.capture.CaptureLocationLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureLocationTextTest {
    @Test
    fun androidTenAndNewerUsePublicPicturesLabel() {
        assertEquals(
            CaptureLocationLabels.STANDARD_PHOTOS,
            CaptureLocationLabels.defaultPhotoLocationLabel(29),
        )
        assertEquals(
            "As fotos aparecem na pasta de imagens do Android.",
            defaultLocationDescription(29),
        )
    }

    @Test
    fun androidSevenToNineUseAppPicturesLabel() {
        assertEquals(
            CaptureLocationLabels.APP_EXTERNAL_PHOTOS,
            CaptureLocationLabels.defaultPhotoLocationLabel(28),
        )
        assertEquals(
            "Neste Android, as fotos ficam na pasta de imagens do app.",
            defaultLocationDescription(28),
        )
    }

    @Test
    fun recordingInfoShowsRecordingAsAvailable() {
        assertEquals(
            listOf(
                "Disponível na tela cheia pelo Menu Rápido.",
                "Use Gravar e Parar.",
                "Formato: MP4, com áudio quando a câmera enviar áudio compatível.",
            ),
            recordingInfoLines(),
        )
    }

    @Test
    fun releaseStorageTextDoesNotOfferCustomLocation() {
        assertEquals(
            listOf(
                "Local personalizado indisponível nesta versão.",
                "Fotos e vídeos usam as pastas padrão do Android.",
            ),
            storageInfoLines(customPhotoLocationEnabled = false),
        )
    }

    @Test
    fun storageInfoCardOnlyAppearsWhenCustomLocationIsEnabled() {
        assertTrue(shouldShowStorageInfoCard(customPhotoLocationEnabled = true))
        assertFalse(shouldShowStorageInfoCard(customPhotoLocationEnabled = false))
    }
}
