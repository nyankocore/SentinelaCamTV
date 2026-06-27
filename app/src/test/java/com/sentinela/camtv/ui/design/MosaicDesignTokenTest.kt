package com.sentinela.camtv.ui.design

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MosaicDesignTokenTest {
    @Test
    fun mosaicTileGapIsCompact() {
        assertEquals(1.dp, SentinelaTvSpacing.mosaicTileGap)
    }
}
