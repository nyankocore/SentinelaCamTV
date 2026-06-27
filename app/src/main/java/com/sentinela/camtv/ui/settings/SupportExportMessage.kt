package com.sentinela.camtv.ui.settings

import com.sentinela.camtv.config.ProjectLinks
import java.io.File

internal object SupportExportMessage {
    fun forExportedFile(file: File): String =
        "Arquivo gerado: ${file.absolutePath}\n\n" +
            "1. Envie esse arquivo para um serviço de nuvem.\n" +
            "2. Copie o link de compartilhamento do arquivo.\n" +
            "3. Acesse:\n" +
            "${ProjectLinks.ISSUES_URL}\n" +
            "4. Crie um relato do problema e cole o link no texto."
}
