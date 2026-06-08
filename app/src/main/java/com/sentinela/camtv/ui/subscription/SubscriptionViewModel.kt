package com.sentinela.camtv.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.entitlement.EntitlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SubscriptionUiState(
    val billing: BillingState = BillingState(),
    val message: String? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
)

class SubscriptionViewModel(
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SubscriptionUiState> = combine(
        entitlementRepository.billingState,
        message,
    ) { billing, currentMessage ->
        SubscriptionUiState(
            billing = billing,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionUiState(),
    )

    fun subscribe(activity: Activity?, plan: SubscriptionPlan) {
        if (activity == null) {
            message.value = "Não foi possível abrir a compra nesta tela."
            return
        }
        val result = entitlementRepository.launchPurchase(activity, plan)
        message.value = result.fold(
            onSuccess = {
                if (BuildConfig.DEBUG) {
                    "Compra simulada neste build."
                } else {
                    "Abrindo compra no Google Play..."
                }
            },
            onFailure = { error -> error.message ?: "Falha ao abrir compra." },
        )
    }

    fun restoreSubscription() {
        message.value = "Restaurando assinatura..."
        entitlementRepository.refresh()
    }

    fun updatePayment() {
        entitlementRepository.refresh()
        message.value = if (BuildConfig.DEBUG) {
            "Atualização de pagamento simulada no debug."
        } else {
            "Restaurando assinatura. A Google Play avisará se houver ação de pagamento."
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
