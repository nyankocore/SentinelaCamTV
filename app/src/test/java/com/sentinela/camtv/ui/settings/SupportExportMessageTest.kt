package com.sentinela.camtv.ui.settings

import com.sentinela.camtv.config.ProjectLinks
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportExportMessageTest {
    @Test
    fun exportedFileMessageIncludesIssuesInstructions() {
        val message = SupportExportMessage.forExportedFile(File("sentinela-logs.txt"))

        assertTrue(message.contains("Arquivo gerado:"))
        assertTrue(message.contains("1. Envie esse arquivo para um serviço de nuvem."))
        assertTrue(message.contains("2. Copie o link de compartilhamento do arquivo."))
        assertTrue(message.contains("4. Crie um relato do problema e cole o link no texto."))
        assertTrue(message.contains(ProjectLinks.ISSUES_URL))
    }
}
