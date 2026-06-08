package com.sentinela.camtv.debug

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class DebugFeatureState(
    val homeActionLabel: String? = null,
    val quickMenuActionLabel: String? = null,
    val footerSuffix: String? = null,
)

interface DebugFeatureProvider {
    val state: StateFlow<DebugFeatureState>
    fun openPanel()

    @Composable
    fun Render()
}
