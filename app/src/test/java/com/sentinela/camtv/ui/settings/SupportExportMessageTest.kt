package com.sentinela.camtv.ui.settings

import com.sentinela.camtv.R
import com.sentinela.camtv.config.ProjectLinks
import com.sentinela.camtv.ui.text.UiText
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportExportMessageTest {
    @Test
    fun exportedFileMessageIncludesIssuesInstructions() {
        val file = File("sentinela-logs.txt")
        val message = SupportExportMessage.forExportedFile(file)

        assertTrue(message is UiText.Resource)
        message as UiText.Resource
        assertEquals(R.string.support_export_message, message.id)
        assertEquals(file.absolutePath, message.args[0])
        assertEquals(ProjectLinks.ISSUES_URL, message.args[1])
    }
}
