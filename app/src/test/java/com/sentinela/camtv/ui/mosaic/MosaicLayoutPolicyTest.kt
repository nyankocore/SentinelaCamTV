package com.sentinela.camtv.ui.mosaic

import com.sentinela.camtv.player.StreamQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MosaicLayoutPolicyTest {
    @Test
    fun fourCamerasUseTwoByTwoLayout() {
        assertEquals(listOf(2, 2), MosaicLayoutPolicy.rowCounts(4))
    }

    @Test
    fun threeCamerasUseTwoPlusOneLayout() {
        assertEquals(listOf(2, 1), MosaicLayoutPolicy.rowCounts(3))
    }

    @Test
    fun fiveCamerasKeepThreePlusTwoLayout() {
        assertEquals(listOf(3, 2), MosaicLayoutPolicy.rowCounts(5))
    }

    @Test
    fun sixteenCamerasUseAtMostFourColumns() {
        val rows = MosaicLayoutPolicy.rowCounts(16)

        assertEquals(listOf(4, 4, 4, 4), rows)
        assertTrue(rows.all { count -> count <= 4 })
    }

    @Test
    fun realAspectRatioReplacesStreamQualityFallback() {
        val aspectRatios = mapOf(MosaicAspectRatioKey("cam-1", StreamQuality.SD.subtype) to 16f / 9f)

        assertEquals(
            16f / 9f,
            MosaicLayoutPolicy.aspectRatioFor(
                cameraId = "cam-1",
                subtype = StreamQuality.SD.subtype,
                streamQuality = StreamQuality.SD,
                aspectRatios = aspectRatios,
            ),
            0.0001f,
        )
    }

    @Test
    fun missingAspectRatioUsesGenericFallback() {
        assertEquals(
            4f / 3f,
            MosaicLayoutPolicy.aspectRatioFor(
                cameraId = "cam-1",
                subtype = StreamQuality.SD.subtype,
                streamQuality = StreamQuality.SD,
                aspectRatios = emptyMap(),
            ),
            0.0001f,
        )
        assertEquals(
            16f / 9f,
            MosaicLayoutPolicy.aspectRatioFor(
                cameraId = "cam-1",
                subtype = StreamQuality.HD.subtype,
                streamQuality = StreamQuality.HD,
                aspectRatios = emptyMap(),
            ),
            0.0001f,
        )
    }

    @Test
    fun invalidVideoSizeIsIgnored() {
        assertNull(MosaicLayoutPolicy.validatedAspectRatio(0, 720))
        assertNull(MosaicLayoutPolicy.validatedAspectRatio(1280, 0))
    }

    @Test
    fun calculatedTilesStayInsideAvailableArea() {
        val layout = MosaicLayoutPolicy.calculate(
            cameraCount = 4,
            availableWidth = 1280f,
            availableHeight = 720f,
            gap = 4f,
            aspectRatios = List(4) { 16f / 9f },
        )

        assertEquals(4, layout.tiles.size)
        assertTrue(layout.tiles.all { tile ->
            tile.x >= 0f &&
                tile.y >= 0f &&
                tile.x + tile.width <= 1280.01f &&
                tile.y + tile.height <= 720.01f
        })
    }

    @Test
    fun threeCameraSingleRowIsCenteredAtSameHeightAsDenseRow() {
        val layout = MosaicLayoutPolicy.calculate(
            cameraCount = 3,
            availableWidth = 1280f,
            availableHeight = 720f,
            gap = 4f,
            aspectRatios = List(3) { 16f / 9f },
        )

        val bottomTile = layout.tiles.last()
        assertTrue(bottomTile.x > 0f)
        assertEquals(layout.tiles.first().height, bottomTile.height, 0.0001f)
    }
}
