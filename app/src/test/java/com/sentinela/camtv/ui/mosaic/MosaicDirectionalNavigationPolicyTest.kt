package com.sentinela.camtv.ui.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MosaicDirectionalNavigationPolicyTest {
    @Test
    fun fiveCameraLayoutMovesBottomLeftTileUpToTopLeftTile() {
        val layout = fiveCameraLayout()

        assertEquals(
            0,
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 3,
                direction = MosaicNavigationDirection.Up,
            ),
        )
    }

    @Test
    fun fiveCameraLayoutMovesBottomRightTileUpToTopRightTile() {
        val layout = fiveCameraLayout()

        assertEquals(
            2,
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 4,
                direction = MosaicNavigationDirection.Up,
            ),
        )
    }

    @Test
    fun horizontalDirectionsMoveInsideSameVisualRow() {
        val layout = fiveCameraLayout()

        assertEquals(
            1,
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 0,
                direction = MosaicNavigationDirection.Right,
            ),
        )
        assertEquals(
            3,
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 4,
                direction = MosaicNavigationDirection.Left,
            ),
        )
    }

    @Test
    fun directionWithoutNeighborReturnsNull() {
        val layout = fiveCameraLayout()

        assertNull(
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 0,
                direction = MosaicNavigationDirection.Up,
            ),
        )
        assertNull(
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 0,
                direction = MosaicNavigationDirection.Left,
            ),
        )
    }

    @Test
    fun visualTieChoosesLeftMostCandidate() {
        val layout = fiveCameraLayout()

        assertEquals(
            3,
            MosaicDirectionalNavigationPolicy.targetIndex(
                tiles = layout.tiles,
                currentIndex = 1,
                direction = MosaicNavigationDirection.Down,
            ),
        )
    }

    private fun fiveCameraLayout(): MosaicLayout =
        MosaicLayoutPolicy.calculate(
            cameraCount = 5,
            availableWidth = 1280f,
            availableHeight = 720f,
            gap = 1f,
            aspectRatios = List(5) { 4f / 3f },
        )
}
