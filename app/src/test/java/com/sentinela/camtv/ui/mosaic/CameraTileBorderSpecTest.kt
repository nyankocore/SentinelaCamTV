package com.sentinela.camtv.ui.mosaic

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class CameraTileBorderSpecTest {
    @Test
    fun unfocusedTileHasNoPermanentBorder() {
        assertNull(
            cameraTileBorderSpec(
                focused = false,
                selectedForReorder = false,
            ),
        )
    }

    @Test
    fun focusedAndSelectedTilesKeepVisibleBorders() {
        val focused = cameraTileBorderSpec(
            focused = true,
            selectedForReorder = false,
        )
        val selected = cameraTileBorderSpec(
            focused = false,
            selectedForReorder = true,
        )

        assertNotNull(focused)
        assertNotNull(selected)
        assertEquals(4.dp, focused?.width)
        assertEquals(4.dp, selected?.width)
    }
}
