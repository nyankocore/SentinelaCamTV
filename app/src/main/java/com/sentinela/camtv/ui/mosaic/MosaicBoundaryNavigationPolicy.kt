package com.sentinela.camtv.ui.mosaic

import com.sentinela.camtv.data.mosaic.MOSAIC_COUNT

internal data class MosaicSwitchTarget(
    val fromIndex: Int,
    val toIndex: Int,
    val direction: MosaicNavigationDirection,
)

internal object MosaicBoundaryNavigationPolicy {
    fun switchTarget(
        activeMosaicIndex: Int,
        direction: MosaicNavigationDirection,
    ): MosaicSwitchTarget? {
        if (direction != MosaicNavigationDirection.Left && direction != MosaicNavigationDirection.Right) {
            return null
        }
        val safeIndex = activeMosaicIndex.coerceIn(0, MOSAIC_COUNT - 1)
        val targetIndex = when (direction) {
            MosaicNavigationDirection.Left -> (safeIndex - 1 + MOSAIC_COUNT) % MOSAIC_COUNT
            MosaicNavigationDirection.Right -> (safeIndex + 1) % MOSAIC_COUNT
            else -> return null
        }
        return MosaicSwitchTarget(
            fromIndex = safeIndex,
            toIndex = targetIndex,
            direction = direction,
        )
    }

    fun isBoundaryTile(
        tiles: List<MosaicTileBounds>,
        currentIndex: Int,
        direction: MosaicNavigationDirection,
    ): Boolean {
        if (direction != MosaicNavigationDirection.Left && direction != MosaicNavigationDirection.Right) {
            return false
        }
        val current = tiles.firstOrNull { tile -> tile.index == currentIndex } ?: return false
        return tiles.none { tile ->
            tile.index != current.index &&
                tile.isHorizontalNeighborOf(current, direction)
        }
    }

    private fun MosaicTileBounds.isHorizontalNeighborOf(
        current: MosaicTileBounds,
        direction: MosaicNavigationDirection,
    ): Boolean {
        val sameVisualRow = intervalGap(
            start = y,
            end = y + height,
            otherStart = current.y,
            otherEnd = current.y + current.height,
        ) == 0f
        if (!sameVisualRow) return false
        return when (direction) {
            MosaicNavigationDirection.Left -> x + width / 2f < current.x + current.width / 2f
            MosaicNavigationDirection.Right -> x + width / 2f > current.x + current.width / 2f
            else -> false
        }
    }

    private fun intervalGap(
        start: Float,
        end: Float,
        otherStart: Float,
        otherEnd: Float,
    ): Float = when {
        end < otherStart -> otherStart - end
        otherEnd < start -> start - otherEnd
        else -> 0f
    }
}
