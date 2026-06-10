package com.sentinela.camtv.ui.mosaic

import org.junit.Assert.assertEquals
import org.junit.Test

class MosaicUiTextTest {
    @Test
    fun reorderHintExplainsSwapDeleteAndBack() {
        assertEquals(
            "Selecione duas câmeras para trocar. Pressione OK por alguns segundos para remover uma câmera do mosaico. Pressione Back para concluir.",
            MosaicUiText.REORDER_HINT,
        )
    }

    @Test
    fun removalConfirmationKeepsCameraRegistered() {
        assertEquals(
            "Remover do mosaico?",
            MosaicUiText.REMOVE_CAMERA_FROM_MOSAIC_CONFIRMATION,
        )
        assertEquals(
            "A câmera continuará cadastrada.",
            MosaicUiText.REMOVE_CAMERA_FROM_MOSAIC_MESSAGE,
        )
    }
}
