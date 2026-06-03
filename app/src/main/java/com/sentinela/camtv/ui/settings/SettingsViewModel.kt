package com.sentinela.camtv.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.billing.SubscriptionAccess
import com.sentinela.camtv.entitlement.EntitlementRepository
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val message: String? = null,
    val billing: BillingState = BillingState(),
    val diagnosticsEnabled: Boolean = true,
    val versionName: String = BuildConfig.VERSION_NAME,
) {
    val accessLabel: String =
        when (billing.access) {
            SubscriptionAccess.Premium -> "Assinatura ativa"
            SubscriptionAccess.Trial -> "Teste grátis ativo"
            SubscriptionAccess.FreeLimited -> "Modo grátis: 1 câmera ativa"
            SubscriptionAccess.BillingUnavailable -> "Google Play Billing indisponível"
        }
}

class SettingsViewModel(
    private val entitlementRepository: EntitlementRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        message,
        entitlementRepository.observeEntitlement(),
        settingsRepository.observePreferences(),
    ) { currentMessage, entitlement, preferences ->
        SettingsUiState(
            message = currentMessage ?: entitlement.billing.message,
            billing = entitlement.billing,
            diagnosticsEnabled = preferences.diagnosticsEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun subscribeMonthly(activity: Activity?) {
        launchPurchase(activity, SubscriptionPlan.Monthly)
    }

    fun subscribeAnnual(activity: Activity?) {
        launchPurchase(activity, SubscriptionPlan.Annual)
    }

    fun restoreSubscription() {
        message.value = "Restaurando assinatura..."
        entitlementRepository.refresh()
    }

    fun toggleDiagnostics() {
        viewModelScope.launch {
            val nextEnabled = !state.value.diagnosticsEnabled
            settingsRepository.setDiagnosticsEnabled(nextEnabled)
            message.value = if (nextEnabled) {
                "Diagnóstico automático ativado."
            } else {
                "Diagnóstico automático desativado."
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun launchPurchase(activity: Activity?, plan: SubscriptionPlan) {
        if (activity == null) {
            message.value = "Não foi possível abrir a compra nesta tela."
            return
        }
        val result = entitlementRepository.launchPurchase(activity, plan)
        message.value = result.fold(
            onSuccess = {
                if (BuildConfig.DEBUG) {
                    "Assinatura simulada no debug."
                } else {
                    "Abrindo compra no Google Play..."
                }
            },
            onFailure = { error -> error.message ?: "Falha ao abrir compra." },
        )
    }
}
