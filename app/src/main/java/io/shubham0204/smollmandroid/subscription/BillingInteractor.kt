package io.shubham0204.smollmandroid.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
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
import io.shubham0204.smollmandroid.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

private const val TAG = "BillingInteractor"

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
        if (BuildConfig.BILLING_BYPASS) {
            Log.w(TAG, "Billing bypass enabled (debug build). Treating as ready.")
            ready = true
            _priceFlow.value = "$9.99" // static test price
            onReady()
            return
        }
        if (ready) { onReady(); return }
        Log.d(TAG, "Starting billing connection...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully.")
                    ready = true
                    queryProductDetails(onReady)
                } else {
                    Log.e(TAG, "Billing setup failed! Response code: ${result.responseCode}, Message: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected.")
                ready = false
            }
        })
    }

    private fun queryProductDetails(onReady: () -> Unit) {
        if (BuildConfig.BILLING_BYPASS) { onReady(); return }
        Log.d(TAG, "Querying product details for ${SubscriptionPolicy.PRODUCT_ID_ANNUAL}...")
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SubscriptionPolicy.PRODUCT_ID_ANNUAL)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )).build()
        billingClient.queryProductDetailsAsync(params) { br, list ->
            if (br.responseCode == BillingClient.BillingResponseCode.OK && list.isNotEmpty()) {
                Log.d(TAG, "Product details query successful.")
                productDetails = list.first()
                val offer = productDetails?.subscriptionOfferDetails?.firstOrNull()
                val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                _priceFlow.value = phase?.formattedPrice
            } else {
                Log.e(TAG, "Product details query failed! Response code: ${br.responseCode}, Message: ${br.debugMessage}, List size: ${list.size}")
            }
            onReady()
        }
    }

    fun launchPurchase(activity: Activity, onResult: (PurchaseOutcome) -> Unit) {
        if (BuildConfig.BILLING_BYPASS) {
            Log.w(TAG, "Bypass purchase: immediately returning success.")
            onResult(PurchaseOutcome.Success)
            return
        }
        val pd = productDetails
        if (!ready || pd == null) {
            Log.e(TAG, "Launch purchase failed: Billing not ready. Ready state: $ready, ProductDetails null: ${pd == null}")
            onResult(PurchaseOutcome.Error("Billing not ready"));
            return
        }
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
        if (BuildConfig.BILLING_BYPASS) { onResult(null); return }
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
        if (BuildConfig.BILLING_BYPASS) return
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (p.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(p.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (BuildConfig.BILLING_BYPASS) return
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