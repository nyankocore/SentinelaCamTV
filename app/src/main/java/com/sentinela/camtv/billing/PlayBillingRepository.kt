package com.sentinela.camtv.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class PlayBillingRepository(
    context: Context,
) : BillingRepository, PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(BillingState(loading = true))
    override val state: StateFlow<BillingState> = _state

    private var productDetails: ProductDetails? = null
    private var started = false
    private var activePurchaseToken: String? = null
    private var pendingPlan: SubscriptionPlan? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    override fun start() {
        if (started) return
        started = true
        connect()
    }

    override fun refresh() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
        } else {
            connect()
        }
    }

    override fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit> {
        val details = productDetails
            ?: return Result.failure(IllegalStateException("Assinatura ainda indisponivel."))
        val offer = state.value.offerFor(plan)
            ?: return Result.failure(IllegalStateException("Plano ${plan.basePlanId} indisponivel."))

        if (state.value.hasFullAccess && state.value.activePlan == plan) {
            return Result.failure(IllegalStateException("Este plano já está ativo."))
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val paramsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
        activePurchaseToken?.let { token ->
            paramsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(token)
                    .build(),
            )
        }
        val params = paramsBuilder.build()
        val result = billingClient.launchBillingFlow(activity, params)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            pendingPlan = plan
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(result.debugMessage.ifBlank { "Falha ao abrir compra." }))
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.value = _state.value.copy(
                loading = false,
                message = billingResult.debugMessage.ifBlank { "Falha ao atualizar assinatura." },
            )
        }
    }

    private fun connect() {
        _state.value = _state.value.copy(loading = true)
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        _state.value = BillingState(
                            access = SubscriptionAccess.BillingUnavailable,
                            loading = false,
                            message = billingResult.debugMessage.ifBlank { "Google Play Billing indisponivel." },
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _state.value = _state.value.copy(
                        access = SubscriptionAccess.BillingUnavailable,
                        loading = false,
                        message = "Google Play Billing desconectado.",
                    )
                }
            },
        )
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(SubscriptionOffer.PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, products ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    loading = false,
                    message = billingResult.debugMessage.ifBlank { "Nao foi possivel carregar planos." },
                )
                return@queryProductDetailsAsync
            }

            val details = products.productDetailsList.firstOrNull()
            productDetails = details
            _state.value = _state.value.copy(
                monthlyOffer = details?.offerFor(SubscriptionPlan.Monthly),
                annualOffer = details?.offerFor(SubscriptionPlan.Annual),
                loading = false,
            )
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            } else {
                _state.value = _state.value.copy(
                    access = SubscriptionAccess.BillingUnavailable,
                    loading = false,
                    message = billingResult.debugMessage.ifBlank { "Nao foi possivel restaurar assinatura." },
                )
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Timber.tag("SentinelaBilling").w(
                            "Falha ao reconhecer assinatura: ${result.debugMessage}",
                        )
                    }
                }
            }
        }

        val activePurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                SubscriptionOffer.PRODUCT_ID in purchase.products
        }
        val active = activePurchase != null
        activePurchaseToken = activePurchase?.purchaseToken
        val nextActivePlan = if (active) {
            pendingPlan ?: _state.value.activePlan
        } else {
            null
        }
        pendingPlan = null
        _state.value = _state.value.copy(
            access = if (active) SubscriptionAccess.Premium else SubscriptionAccess.FreeLimited,
            activePlan = nextActivePlan,
            loading = false,
        )
    }
}

private fun BillingState.offerFor(plan: SubscriptionPlan): SubscriptionOffer? =
    when (plan) {
        SubscriptionPlan.Monthly -> monthlyOffer
        SubscriptionPlan.Annual -> annualOffer
    }

private fun ProductDetails.offerFor(plan: SubscriptionPlan): SubscriptionOffer? =
    subscriptionOfferDetails
        ?.firstOrNull { offer -> offer.basePlanId == plan.basePlanId }
        ?.let { offer ->
            val phase = offer.pricingPhases.pricingPhaseList.lastOrNull()
            SubscriptionOffer(
                plan = plan,
                formattedPrice = phase?.formattedPrice ?: "",
                billingPeriod = phase?.billingPeriod ?: "",
                offerToken = offer.offerToken,
                productId = productId,
            )
        }
