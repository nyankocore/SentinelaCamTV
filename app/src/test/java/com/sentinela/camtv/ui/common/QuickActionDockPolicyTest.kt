package com.sentinela.camtv.ui.common

import androidx.compose.ui.unit.dp
import com.sentinela.camtv.player.TransmissionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionDockPolicyTest {
    @Test
    fun transmissionModesUseTheirApprovedIcons() {
        assertEquals(
            QuickActionIcon.ModeEthernet,
            TransmissionMode.MENOR_LATENCIA.quickActionModeIcon(),
        )
        assertEquals(
            QuickActionIcon.ModeStability,
            TransmissionMode.QUALIDADE.quickActionModeIcon(),
        )
    }

    @Test
    fun dockScrollsOnlyWhenActionsExceedSafeWidth() {
        assertFalse(
            quickActionDockNeedsScrolling(
                availableWidth = 960.dp,
                actionWidths = listOf(100.dp, 100.dp, 100.dp),
            ),
        )
        assertTrue(
            quickActionDockNeedsScrolling(
                availableWidth = 960.dp,
                actionWidths = List(8) { 130.dp },
            ),
        )
        assertEquals(
            928.dp,
            quickActionDockWidth(
                availableWidth = 960.dp,
                actionWidths = List(8) { 130.dp },
            ),
        )
    }

    @Test
    fun dockScrollsAtVisibleEdgesInsteadOfEveryFocusMove() {
        assertNull(
            quickActionDockScrollAnchor(
                currentIndex = 2,
                targetIndex = 3,
                actionCount = 8,
                firstVisibleIndex = 0,
                lastVisibleIndex = 6,
            ),
        )
        assertEquals(
            7,
            quickActionDockScrollAnchor(
                currentIndex = 5,
                targetIndex = 6,
                actionCount = 8,
                firstVisibleIndex = 0,
                lastVisibleIndex = 6,
            ),
        )
        assertEquals(
            0,
            quickActionDockScrollAnchor(
                currentIndex = 2,
                targetIndex = 1,
                actionCount = 8,
                firstVisibleIndex = 1,
                lastVisibleIndex = 7,
            ),
        )
    }

    @Test
    fun dockDoesNotScrollWhenAllActionsAreVisible() {
        assertNull(
            quickActionDockScrollAnchor(
                currentIndex = 4,
                targetIndex = 5,
                actionCount = 6,
                firstVisibleIndex = 0,
                lastVisibleIndex = 5,
            ),
        )
        assertNull(
            quickActionDockScrollAnchor(
                currentIndex = 1,
                targetIndex = 0,
                actionCount = 6,
                firstVisibleIndex = 0,
                lastVisibleIndex = 5,
            ),
        )
    }

    @Test
    fun ltrFocusStaysInsideDockAtBothEdges() {
        assertNull(
            quickActionDockTargetIndex(
                currentIndex = 0,
                actionCount = 8,
                direction = QuickActionDockDirection.Left,
                isRtl = false,
            ),
        )
        assertEquals(
            1,
            quickActionDockTargetIndex(
                currentIndex = 0,
                actionCount = 8,
                direction = QuickActionDockDirection.Right,
                isRtl = false,
            ),
        )
        assertNull(
            quickActionDockTargetIndex(
                currentIndex = 7,
                actionCount = 8,
                direction = QuickActionDockDirection.Right,
                isRtl = false,
            ),
        )
    }

    @Test
    fun rtlFocusUsesTheVisualDirectionAndStaysInsideDock() {
        assertNull(
            quickActionDockTargetIndex(
                currentIndex = 0,
                actionCount = 8,
                direction = QuickActionDockDirection.Right,
                isRtl = true,
            ),
        )
        assertEquals(
            1,
            quickActionDockTargetIndex(
                currentIndex = 0,
                actionCount = 8,
                direction = QuickActionDockDirection.Left,
                isRtl = true,
            ),
        )
    }
}
