package com.sentinela.camtv.billing

enum class SubscriptionPlan(
    val basePlanId: String,
) {
    Monthly("monthly"),
    Annual("annual"),
}

data class SubscriptionOffer(
    val plan: SubscriptionPlan,
    val formattedPrice: String,
    val billingPeriod: String,
    val offerToken: String,
    val productId: String = PRODUCT_ID,
) {
    companion object {
        const val PRODUCT_ID = "sentinela_plus"
    }
}

enum class SubscriptionAccess {
    Premium,
    Trial,
    FreeLimited,
    BillingUnavailable,
}

data class BillingState(
    val access: SubscriptionAccess = SubscriptionAccess.FreeLimited,
    val monthlyOffer: SubscriptionOffer? = null,
    val annualOffer: SubscriptionOffer? = null,
    val activePlan: SubscriptionPlan? = null,
    val loading: Boolean = false,
    val message: String? = null,
) {
    val hasFullAccess: Boolean
        get() = access == SubscriptionAccess.Premium || access == SubscriptionAccess.Trial
}
