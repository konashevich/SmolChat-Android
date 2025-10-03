package io.shubham0204.smollmandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.shubham0204.smollmandroid.subscription.BillingInteractor
import io.shubham0204.smollmandroid.subscription.EntitlementState
import io.shubham0204.smollmandroid.subscription.SubscriptionManager
import io.shubham0204.smollmandroid.subscription.PurchaseOutcome
import org.koin.android.ext.android.inject

/**
 * Simple initial paywall screen. Real implementation will use BillingClient purchase flow.
 */
class PaywallActivity : ComponentActivity() {
    private val subscriptionManager by inject<SubscriptionManager>()
    private val billingInteractor by inject<BillingInteractor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        subscriptionManager.evaluateState()
        // Bypass paywall entirely if build config flag enabled (debug builds)
        if (BuildConfig.BILLING_BYPASS) {
            navigateForward(); return
        }
        if (subscriptionManager.currentState() != EntitlementState.NOT_ENTITLED) {
            navigateForward(); return
        }
        setContent {
            val price = billingInteractor.priceFlow.collectAsState().value
            val entitlement = subscriptionManager.stateFlow.collectAsState().value
            var purchaseMessage by remember { mutableStateOf<String?>(null) }
            if (entitlement != EntitlementState.NOT_ENTITLED) {
                LaunchedEffect(entitlement) { navigateForward() }
            }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaywallScreen(
                        dynamicPrice = price,
                        onSubscribe = {
                            subscriptionManager.beginPurchase(this@PaywallActivity) { outcome ->
                                purchaseMessage = when (outcome) {
                                    is PurchaseOutcome.Success -> "Purchase successful"
                                    is PurchaseOutcome.Cancelled -> "Purchase cancelled"
                                    is PurchaseOutcome.Error -> "Error: ${outcome.message}"
                                 }
                            }
                        },
                        onRestore = { subscriptionManager.refreshIfNeeded(force = true) },
                        onExit = { finish() },
                        statusMessage = purchaseMessage,
                        onEthics = { startActivity(Intent(this@PaywallActivity, EthicsActivity::class.java)) },
                    )
                }
            }
        }
    }

    private fun navigateForward() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}

@Composable
private fun PaywallScreen(
    dynamicPrice: String?,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    onExit: () -> Unit,
    statusMessage: String?,
    onEthics: () -> Unit,
) {
    val priceText = dynamicPrice ?: "$9.99 / year"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Unlock Crisis AI",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Annual subscription $priceText. Immediate charge. 30-day money-back on request. If global networks fail, the app continues in Survival Mode.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) { Text("Subscribe Now") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore Access") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Exit") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onEthics, modifier = Modifier.fillMaxWidth()) { Text("Ethics / Survival Policy") }
        statusMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}