package com.sentinela.camtv.ui.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MosaicBoundaryNavigationPolicyTest {
    @Test
    fun horizontalSwitchesWrapAcrossThreeMosaics() {
        assertEquals(
            2,
            MosaicBoundaryNavigationPolicy.switchTarget(0, MosaicNavigationDirection.Left)?.toIndex,
        )
        assertEquals(
            1,
            MosaicBoundaryNavigationPolicy.switchTarget(0, MosaicNavigationDirection.Right)?.toIndex,
        )
        assertEquals(
            0,
            MosaicBoundaryNavigationPolicy.switchTarget(1, MosaicNavigationDirection.Left)?.toIndex,
        )
        assertEquals(
            0,
            MosaicBoundaryNavigationPolicy.switchTarget(2, MosaicNavigationDirection.Right)?.toIndex,
        )
    }

    @Test
    fun verticalDirectionsDoNotSwitchMosaics() {
        assertNull(MosaicBoundaryNavigationPolicy.switchTarget(0, MosaicNavigationDirection.Up))
        assertNull(MosaicBoundaryNavigationPolicy.switchTarget(0, MosaicNavigationDirection.Down))
    }

    @Test
    fun emptyMosaicCanUseHorizontalSwitchPolicy() {
        assertEquals(
            MosaicSwitchTarget(
                fromIndex = 1,
                toIndex = 0,
                direction = MosaicNavigationDirection.Left,
            ),
            MosaicBoundaryNavigationPolicy.switchTarget(1, MosaicNavigationDirection.Left),
        )
        assertEquals(
            MosaicSwitchTarget(
                fromIndex = 1,
                toIndex = 2,
                direction = MosaicNavigationDirection.Right,
            ),
            MosaicBoundaryNavigationPolicy.switchTarget(1, MosaicNavigationDirection.Right),
        )
    }

    @Test
    fun fiveCameraLayoutDetectsLeftAndRightBoundaryTiles() {
        val layout = fiveCameraLayout()

        assertTrue(
            MosaicBoundaryNavigationPolicy.isBoundaryTile(
                tiles = layout.tiles,
                currentIndex = 2,
                direction = MosaicNavigationDirection.Right,
            ),
        )
        assertTrue(
            MosaicBoundaryNavigationPolicy.isBoundaryTile(
                tiles = layout.tiles,
                currentIndex = 4,
                direction = MosaicNavigationDirection.Right,
            ),
        )
        assertFalse(
            MosaicBoundaryNavigationPolicy.isBoundaryTile(
                tiles = layout.tiles,
                currentIndex = 1,
                direction = MosaicNavigationDirection.Right,
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
