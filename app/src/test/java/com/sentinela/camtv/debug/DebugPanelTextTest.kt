package com.sentinela.camtv.debug

import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.billing.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugPanelTextTest {
    @Test
    fun performanceSectionIsNotShown() {
        assertFalse(debugSectionLabels().contains("Performance"))
    }

    @Test
    fun detailAppearsOnlyForCurrentSection() {
        assertTrue(
            shouldShowDebugDetail(
                selectedSectionLabel = "Informações técnicas",
                detailSectionLabel = "Informações técnicas",
            ),
        )
        assertFalse(
            shouldShowDebugDetail(
                selectedSectionLabel = "Assinatura",
                detailSectionLabel = "Informações técnicas",
            ),
        )
    }

    @Test
    fun technicalInfoUsesGroupedPortugueseText() {
        val text = formatDebugTechnicalInfo(
            DebugTechnicalInfoSnapshot(
                versionName = "2.0.0-debug",
                versionCode = 6,
                packageName = "com.sentinela.camtv.debug",
                buildType = "debug",
                billingLabel = "simulado",
                statusLabel = SubscriptionStatus.CanceledUntilExpiry.debugPortugueseLabel(),
                statusIdentifier = "CanceledUntilExpiry",
                activePlanLabel = SubscriptionPlan.Monthly.debugPortuguesePlanLabel(),
                activeBasePlan = "monthly",
                manufacturer = "SDMC",
                model = "IZY01",
                androidRelease = "10",
                apiLevel = 29,
                abi = "armeabi-v7a",
                screenWidthPx = 1280,
                screenHeightPx = 720,
                availableRamMb = 413,
                totalRamMb = 982,
                lowMemory = false,
                lowMemoryThresholdMb = 128,
                crashlyticsConfigured = false,
                diagnosticsEnabled = true,
            ),
        )

        assertTrue(text.contains("Aplicativo"))
        assertTrue(text.contains("Código da versão: 6"))
        assertTrue(text.contains("Pacote: com.sentinela.camtv.debug"))
        assertTrue(text.contains("Assinatura"))
        assertTrue(text.contains("Estado: Cancelada até vencer"))
        assertTrue(text.contains("Identificador: CanceledUntilExpiry"))
        assertTrue(text.contains("Plano ativo: mensal"))
        assertTrue(text.contains("Base plan: monthly"))
        assertTrue(text.contains("Dispositivo"))
        assertTrue(text.contains("Android: 10 (API 29)"))
        assertTrue(text.contains("Tela: 1280 × 720 px"))
        assertTrue(text.contains("Memória — instantâneo"))
        assertTrue(text.contains("Memória baixa: não"))
        assertTrue(text.contains("Limite de memória baixa: 128 MB"))
        assertTrue(text.contains("Crashlytics: não configurado"))
        assertTrue(text.contains("Diagnóstico automático: ativado."))
        assertEquals("nenhum", (null as SubscriptionPlan?).debugPortuguesePlanLabel())
    }
}
