package com.sentinela.camtv.ui.mosaic

enum class MosaicNavigationDirection {
    Up,
    Down,
    Left,
    Right,
}

internal object MosaicDirectionalNavigationPolicy {
    fun targetIndex(
        tiles: List<MosaicTileBounds>,
        currentIndex: Int,
        direction: MosaicNavigationDirection,
    ): Int? {
        val current = tiles.firstOrNull { tile -> tile.index == currentIndex } ?: return null
        return tiles
            .asSequence()
            .filter { tile -> tile.index != current.index }
            .filter { tile -> tile.isInDirectionFrom(current, direction) }
            .map { tile -> Candidate(tile = tile, score = current.scoreTo(tile, direction)) }
            .minWithOrNull(
                compareBy<Candidate> { candidate -> candidate.score.perpendicularGap }
                    .thenBy { candidate -> candidate.score.totalDistanceSquared }
                    .thenBy { candidate -> candidate.score.perpendicularDistance }
                    .thenBy { candidate -> candidate.tile.x }
                    .thenBy { candidate -> candidate.tile.y }
                    .thenBy { candidate -> candidate.tile.index },
            )
            ?.tile
            ?.index
    }

    private data class Candidate(
        val tile: MosaicTileBounds,
        val score: NavigationScore,
    )

    private data class NavigationScore(
        val perpendicularGap: Float,
        val totalDistanceSquared: Float,
        val perpendicularDistance: Float,
    )

    private fun MosaicTileBounds.scoreTo(
        target: MosaicTileBounds,
        direction: MosaicNavigationDirection,
    ): NavigationScore {
        val primaryDistance = when (direction) {
            MosaicNavigationDirection.Up,
            MosaicNavigationDirection.Down -> (target.centerY - centerY).abs()

            MosaicNavigationDirection.Left,
            MosaicNavigationDirection.Right -> (target.centerX - centerX).abs()
        }
        val perpendicularDistance = when (direction) {
            MosaicNavigationDirection.Up,
            MosaicNavigationDirection.Down -> (target.centerX - centerX).abs()

            MosaicNavigationDirection.Left,
            MosaicNavigationDirection.Right -> (target.centerY - centerY).abs()
        }
        val perpendicularGap = when (direction) {
            MosaicNavigationDirection.Up,
            MosaicNavigationDirection.Down -> horizontalGapTo(target)

            MosaicNavigationDirection.Left,
            MosaicNavigationDirection.Right -> verticalGapTo(target)
        }
        return NavigationScore(
            perpendicularGap = perpendicularGap,
            totalDistanceSquared = primaryDistance * primaryDistance +
                perpendicularDistance * perpendicularDistance,
            perpendicularDistance = perpendicularDistance,
        )
    }

    private fun MosaicTileBounds.isInDirectionFrom(
        current: MosaicTileBounds,
        direction: MosaicNavigationDirection,
    ): Boolean = when (direction) {
        MosaicNavigationDirection.Up -> centerY < current.centerY
        MosaicNavigationDirection.Down -> centerY > current.centerY
        MosaicNavigationDirection.Left -> centerX < current.centerX
        MosaicNavigationDirection.Right -> centerX > current.centerX
    }

    private val MosaicTileBounds.centerX: Float
        get() = x + width / 2f

    private val MosaicTileBounds.centerY: Float
        get() = y + height / 2f

    private fun MosaicTileBounds.horizontalGapTo(other: MosaicTileBounds): Float =
        intervalGap(
            start = x,
            end = x + width,
            otherStart = other.x,
            otherEnd = other.x + other.width,
        )

    private fun MosaicTileBounds.verticalGapTo(other: MosaicTileBounds): Float =
        intervalGap(
            start = y,
            end = y + height,
            otherStart = other.y,
            otherEnd = other.y + other.height,
        )

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

    private fun Float.abs(): Float =
        kotlin.math.abs(this)
}
