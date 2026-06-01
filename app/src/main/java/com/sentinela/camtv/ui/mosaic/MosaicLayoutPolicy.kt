package com.sentinela.camtv.ui.mosaic

import com.sentinela.camtv.player.StreamQuality

internal data class MosaicAspectRatioKey(
    val cameraId: String,
    val subtype: Int,
)

internal data class MosaicTileBounds(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

internal data class MosaicLayout(
    val tiles: List<MosaicTileBounds>,
)

internal object MosaicLayoutPolicy {
    private const val MIN_ASPECT_RATIO = 1f
    private const val MAX_ASPECT_RATIO = 21f / 9f
    private const val HD_FALLBACK_ASPECT_RATIO = 16f / 9f
    private const val SD_FALLBACK_ASPECT_RATIO = 4f / 3f

    fun rowCounts(cameraCount: Int): List<Int> = when {
        cameraCount <= 0 -> emptyList()
        cameraCount == 1 -> listOf(1)
        cameraCount == 2 -> listOf(2)
        cameraCount == 3 -> listOf(2, 1)
        cameraCount == 4 -> listOf(2, 2)
        cameraCount == 5 -> listOf(3, 2)
        cameraCount == 6 -> listOf(3, 3)
        cameraCount == 7 -> listOf(4, 3)
        cameraCount == 8 -> listOf(4, 4)
        cameraCount == 9 -> listOf(3, 3, 3)
        else -> buildList {
            var remaining = cameraCount
            while (remaining > 0) {
                val count = remaining.coerceAtMost(4)
                add(count)
                remaining -= count
            }
        }
    }

    fun validatedAspectRatio(
        width: Int,
        height: Int,
    ): Float? {
        if (width <= 0 || height <= 0) return null
        return (width.toFloat() / height.toFloat()).coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    }

    fun fallbackAspectRatio(streamQuality: StreamQuality): Float = when (streamQuality) {
        StreamQuality.HD -> HD_FALLBACK_ASPECT_RATIO
        StreamQuality.SD -> SD_FALLBACK_ASPECT_RATIO
    }

    fun aspectRatioFor(
        cameraId: String,
        subtype: Int,
        streamQuality: StreamQuality,
        aspectRatios: Map<MosaicAspectRatioKey, Float>,
    ): Float =
        aspectRatios[MosaicAspectRatioKey(cameraId, subtype)]
            ?: fallbackAspectRatio(streamQuality)

    fun calculate(
        cameraCount: Int,
        availableWidth: Float,
        availableHeight: Float,
        gap: Float,
        aspectRatios: List<Float>,
    ): MosaicLayout {
        if (cameraCount <= 0 || availableWidth <= 0f || availableHeight <= 0f) {
            return MosaicLayout(emptyList())
        }

        val rows = rowCounts(cameraCount)
        val safeGap = gap.coerceAtLeast(0f)
        val safeAspectRatios = List(cameraCount) { index ->
            aspectRatios.getOrNull(index)
                ?.takeIf { it > 0f }
                ?.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
                ?: SD_FALLBACK_ASPECT_RATIO
        }
        val maxRowCount = rows.maxOrNull().orEmptyRowCount()
        val rowStartIndexes = rows.runningFold(0) { start, rowCount -> start + rowCount }.dropLast(1)
        val denseReferenceHeight = rows.mapIndexedNotNull { rowIndex, rowCount ->
            if (rowCount != maxRowCount) return@mapIndexedNotNull null
            val startIndex = rowStartIndexes[rowIndex]
            rowHeightForFullWidth(
                rowAspectRatios = safeAspectRatios.subList(startIndex, startIndex + rowCount),
                availableWidth = availableWidth,
                gap = safeGap,
            )
        }.minOrNull() ?: availableHeight

        val desiredRowHeights = rows.mapIndexed { rowIndex, rowCount ->
            val startIndex = rowStartIndexes[rowIndex]
            val fullWidthHeight = rowHeightForFullWidth(
                rowAspectRatios = safeAspectRatios.subList(startIndex, startIndex + rowCount),
                availableWidth = availableWidth,
                gap = safeGap,
            )
            if (rowCount == 1 && cameraCount > 1) {
                fullWidthHeight.coerceAtMost(denseReferenceHeight)
            } else {
                fullWidthHeight
            }
        }

        val totalGapHeight = safeGap * (rows.size - 1).coerceAtLeast(0)
        val desiredContentHeight = desiredRowHeights.sum() + totalGapHeight
        val scale = if (desiredContentHeight > availableHeight && desiredContentHeight > 0f) {
            (availableHeight - totalGapHeight).coerceAtLeast(0f) / desiredRowHeights.sum()
        } else {
            1f
        }
        val rowHeights = desiredRowHeights.map { it * scale }
        val contentHeight = rowHeights.sum() + totalGapHeight
        var y = ((availableHeight - contentHeight) / 2f).coerceAtLeast(0f)
        val tiles = mutableListOf<MosaicTileBounds>()

        rows.forEachIndexed { rowIndex, rowCount ->
            val rowHeight = rowHeights[rowIndex]
            val startIndex = rowStartIndexes[rowIndex]
            val rowRatios = safeAspectRatios.subList(startIndex, startIndex + rowCount)
            val rowWidth = rowRatios.sumOf { (it * rowHeight).toDouble() }.toFloat() +
                safeGap * (rowCount - 1).coerceAtLeast(0)
            var x = ((availableWidth - rowWidth) / 2f).coerceAtLeast(0f)

            rowRatios.forEachIndexed { offset, ratio ->
                val tileWidth = ratio * rowHeight
                tiles += MosaicTileBounds(
                    index = startIndex + offset,
                    x = x,
                    y = y,
                    width = tileWidth,
                    height = rowHeight,
                )
                x += tileWidth + safeGap
            }
            y += rowHeight + safeGap
        }

        return MosaicLayout(tiles)
    }

    private fun rowHeightForFullWidth(
        rowAspectRatios: List<Float>,
        availableWidth: Float,
        gap: Float,
    ): Float {
        val totalGapWidth = gap * (rowAspectRatios.size - 1).coerceAtLeast(0)
        val aspectSum = rowAspectRatios.sum().coerceAtLeast(0.0001f)
        return ((availableWidth - totalGapWidth).coerceAtLeast(0f) / aspectSum)
    }

    private fun Int?.orEmptyRowCount(): Int = this ?: 0
}
