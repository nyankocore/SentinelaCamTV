package com.sentinela.camtv.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun AppInfoFooter(
    versionName: String,
    license: String,
    siteLabel: String,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppInfoFooterText("Versão: $versionName", scale)
        Spacer(Modifier.height(8f.sdp(scale)))
        AppInfoFooterText("Licença: $license", scale)
        Spacer(Modifier.height(8f.sdp(scale)))
        AppInfoFooterText("Site: $siteLabel", scale)
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

private fun Float.sdp(scale: Float): Dp = (this * scale).dp

private fun Float.ssp(scale: Float) = (this * scale).sp
