package com.sentinela.camtv.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun AppInfoFooter(
    versionName: String,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppInfoFooterText("Versão: $versionName", scale)
    }
}

@Composable
private fun AppInfoFooterText(
    text: String,
    scale: Float,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 15f.ssp(scale),
    )
}

private fun Float.ssp(scale: Float) = (this * scale).sp
