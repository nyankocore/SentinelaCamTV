package com.sentinela.camtv.ui.settings

import com.sentinela.camtv.R
import com.sentinela.camtv.config.ProjectLinks
import com.sentinela.camtv.ui.text.UiText
import java.io.File

internal object SupportExportMessage {
    fun forExportedFile(file: File): UiText =
        UiText.Resource(
            R.string.support_export_message,
            listOf(file.absolutePath, ProjectLinks.ISSUES_URL),
        )
}
