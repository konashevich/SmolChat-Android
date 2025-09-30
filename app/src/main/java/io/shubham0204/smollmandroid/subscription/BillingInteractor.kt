package io.shubham0204.smollmandroid.subscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class BillingInteractor(private val context: Context) : PurchasesUpdatedListener {
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    private var productDetails: ProductDetails? = null
    @Volatile private var ready = false

    private val _priceFlow = MutableStateFlow<String?>(null)
    val priceFlow: StateFlow<String?> = _priceFlow

    private val _lastPurchaseResult = MutableStateFlow<Purchase?>(null)
    val lastPurchaseResult: StateFlow<Purchase?> = _lastPurchaseResult

    fun startConnection(onReady: () -> Unit = {}) {
        if (ready) { onReady(); return }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    ready = true
                    queryProductDetails(onReady)
                }
            }
            override fun onBillingServiceDisconnected() { ready = false }
        })
    }

    private fun queryProductDetails(onReady: () -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SubscriptionPolicy.PRODUCT_ID_ANNUAL)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )).build()
        billingClient.queryProductDetailsAsync(params) { br, list ->
            if (br.responseCode == BillingClient.BillingResponseCode.OK && list.isNotEmpty()) {
                productDetails = list.first()
                val offer = productDetails?.subscriptionOfferDetails?.firstOrNull()
                val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                _priceFlow.value = phase?.formattedPrice
            }
            onReady()
        }
    }

    fun launchPurchase(activity: Activity, onResult: (PurchaseOutcome) -> Unit) {
        val pd = productDetails
        if (!ready || pd == null) { onResult(PurchaseOutcome.Error("Billing not ready")); return }
        val offerToken = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) { onResult(PurchaseOutcome.Error("Offer token missing")); return }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(pd)
                    .setOfferToken(offerToken)
                    .build(),
            )).build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onResult(PurchaseOutcome.Error("Launch failed: ${result.debugMessage}"))
        }
    }

    fun queryActivePurchase(onResult: (Purchase?) -> Unit) {
        if (!ready) { startConnection { queryActivePurchase(onResult) }; return }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        ) { br, list ->
            if (br.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(list.firstOrNull { it.products.contains(SubscriptionPolicy.PRODUCT_ID_ANNUAL) })
            } else onResult(null)
        }
    }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (p.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(p.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            val purchase = purchases.first()
            acknowledgeIfNeeded(purchase)
            _lastPurchaseResult.value = purchase
        }
    }
}

sealed class PurchaseOutcome {
    object Success : PurchaseOutcome()
    object Cancelled : PurchaseOutcome()
    data class Error(val message: String) : PurchaseOutcome()
}